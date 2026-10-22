package edu.uet.battlecode.db;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MatchQueueServiceTest {
    private JdbcTemplate jdbc;
    private MatchQueueService queue;
    private UUID matchId;

    @BeforeEach void setUp() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:queue" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE matches (id UUID PRIMARY KEY, status VARCHAR(20), worker_id VARCHAR(100), " +
                "lease_expires_at TIMESTAMP, attempt_count INT DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "started_at TIMESTAMP, completed_at TIMESTAMP, winner_slot INT, replay_data_url VARCHAR(1000), " +
                "error_summary VARCHAR(255))");
        matchId = UUID.randomUUID();
        jdbc.update("INSERT INTO matches(id,status) VALUES (?, 'QUEUED')", matchId);
        queue = new MatchQueueService(source);
    }

    @Test void claimIsExclusiveAndHeartbeatsExtendOnlyCurrentLease() {
        assertEquals(matchId, queue.claim("worker-a").orElseThrow().id());
        assertTrue(queue.claim("worker-b").isEmpty());
        assertFalse(queue.heartbeat(matchId, "worker-b"));
        assertTrue(queue.heartbeat(matchId, "worker-a"));
    }

    @Test void staleWorkerCannotOverwriteCompletedMatch() {
        assertEquals(matchId, queue.claim("worker-a").orElseThrow().id());
        jdbc.update("UPDATE matches SET lease_expires_at = CURRENT_TIMESTAMP - INTERVAL '1' MINUTE WHERE id = ?", matchId);
        assertEquals(matchId, queue.claim("worker-b").orElseThrow().id());
        assertFalse(queue.complete(matchId, "worker-a", 1, "https://r2.example/old.gz", null));
        assertTrue(queue.complete(matchId, "worker-b", 2, "https://r2.example/new.gz", null));
        assertTrue(queue.complete(matchId, "worker-b", 2, "https://r2.example/new.gz", null));
        assertFalse(queue.complete(matchId, "worker-a", 1, "https://r2.example/old.gz", null));
        assertEquals("https://r2.example/new.gz", jdbc.queryForObject(
                "SELECT replay_data_url FROM matches WHERE id = ?", String.class, matchId));
    }

    @Test void systemErrorRetryIsBounded() {
        jdbc.update("UPDATE matches SET status='SYSTEM_ERROR', attempt_count=2 WHERE id=?", matchId);
        assertTrue(queue.retrySystemError(matchId));
        assertEquals(matchId, queue.claim("worker-a").orElseThrow().id());
        jdbc.update("UPDATE matches SET status='SYSTEM_ERROR' WHERE id=?", matchId);
        assertFalse(queue.retrySystemError(matchId));
    }
}
