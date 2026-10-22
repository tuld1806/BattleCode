package edu.uet.battlecode.db;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public final class MatchQueueService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public MatchQueueService(DataSource source) {
        this.jdbc = new JdbcTemplate(source);
        this.transactions = new TransactionTemplate(new JdbcTransactionManager(source));
    }

    public Optional<ClaimedMatch> claim(String workerId) {
        if (workerId == null || workerId.isBlank() || workerId.length() > 100) {
            throw new IllegalArgumentException("Invalid worker id");
        }
        return Objects.requireNonNull(transactions.execute(status -> {
            List<UUID> ids = jdbc.query("SELECT id FROM matches WHERE " +
                    "(status='QUEUED' OR (status='RUNNING' AND lease_expires_at < CURRENT_TIMESTAMP)) " +
                    "AND attempt_count < 3 ORDER BY created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED",
                    (rs, row) -> rs.getObject(1, UUID.class));
            if (ids.isEmpty()) return Optional.<ClaimedMatch>empty();
            UUID id = ids.get(0);
            int changed = jdbc.update("UPDATE matches SET status='RUNNING', worker_id=?, " +
                    "lease_expires_at=CURRENT_TIMESTAMP + INTERVAL '2' MINUTE, " +
                    "attempt_count=attempt_count+1, started_at=COALESCE(started_at,CURRENT_TIMESTAMP) WHERE id=?",
                    workerId, id);
            if (changed != 1) throw new IllegalStateException("Claim lost row lock");
            return jdbc.queryForObject("SELECT id,worker_id,attempt_count,lease_expires_at FROM matches WHERE id=?",
                    (rs, row) -> Optional.of(new ClaimedMatch(rs.getObject("id", UUID.class),
                            rs.getString("worker_id"), rs.getInt("attempt_count"),
                            rs.getTimestamp("lease_expires_at").toInstant())), id);
        }));
    }

    public boolean heartbeat(UUID id, String workerId) {
        return jdbc.update("UPDATE matches SET lease_expires_at=CURRENT_TIMESTAMP + INTERVAL '2' MINUTE " +
                "WHERE id=? AND status='RUNNING' AND worker_id=? AND lease_expires_at > CURRENT_TIMESTAMP",
                id, workerId) == 1;
    }

    public boolean complete(UUID id, String workerId, Integer winnerSlot, String replayUrl, String summary) {
        return complete(id, workerId, winnerSlot, replayUrl, summary, null);
    }

    public boolean complete(UUID id, String workerId, Integer winnerSlot, String replayUrl,
                            String summary, Integer totalTicks) {
        if (winnerSlot != null && winnerSlot != 1 && winnerSlot != 2) throw new IllegalArgumentException("Invalid winner slot");
        if (replayUrl == null || !replayUrl.startsWith("https://")) throw new IllegalArgumentException("Invalid replay URL");
        if (summary != null && summary.length() > 255) throw new IllegalArgumentException("Summary too long");
        int changed = jdbc.update("UPDATE matches SET status='COMPLETED', winner_slot=?, replay_data_url=?, " +
                "error_summary=?, completed_at=CURRENT_TIMESTAMP, lease_expires_at=NULL " +
                "WHERE id=? AND status='RUNNING' AND worker_id=? AND lease_expires_at > CURRENT_TIMESTAMP",
                winnerSlot, replayUrl, summary, id, workerId);
        if (changed == 1 && totalTicks != null) {
            jdbc.update("UPDATE matches SET total_ticks=? WHERE id=?", totalTicks, id);
        }
        if (changed == 1) return true;
        List<Boolean> matching = jdbc.query("SELECT 1 FROM matches WHERE id=? AND status='COMPLETED' " +
                "AND worker_id=? AND winner_slot IS NOT DISTINCT FROM ? AND replay_data_url=? " +
                "AND error_summary IS NOT DISTINCT FROM ?",
                (rs, row) -> true, id, workerId, winnerSlot, replayUrl, summary);
        return !matching.isEmpty();
    }

    public boolean failSystem(UUID id, String workerId, String summary) {
        if (summary == null || summary.length() > 255) throw new IllegalArgumentException("Invalid summary");
        return jdbc.update("UPDATE matches SET status='SYSTEM_ERROR', error_summary=?, lease_expires_at=NULL " +
                "WHERE id=? AND status='RUNNING' AND worker_id=? AND lease_expires_at > CURRENT_TIMESTAMP",
                summary, id, workerId) == 1;
    }

    public boolean retrySystemError(UUID id) {
        return jdbc.update("UPDATE matches SET status='QUEUED', worker_id=NULL, lease_expires_at=NULL " +
                "WHERE id=? AND status='SYSTEM_ERROR' AND attempt_count<3", id) == 1;
    }
}
