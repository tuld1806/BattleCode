package edu.uet.battlecode.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public final class TournamentService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public TournamentService(DataSource source) {
        this.jdbc = new JdbcTemplate(source);
        this.transactions = new TransactionTemplate(new JdbcTransactionManager(source));
    }

    public UUID registerTeam(UUID tournamentId, UUID userId, String name) {
        if (name == null || name.isBlank() || name.length() > 100) throw new IllegalArgumentException("Invalid team name");
        return Objects.requireNonNull(transactions.execute(tx -> {
            requireRegistration(tournamentId);
            Integer total = jdbc.queryForObject("SELECT count(*) FROM teams WHERE tournament_id=?", Integer.class, tournamentId);
            if (total != null && total >= 16) throw new IllegalStateException("Pilot team cap reached");
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO teams(id,tournament_id,name,captain_id) VALUES (?,?,?,?)",
                    id, tournamentId, name.trim(), userId);
            jdbc.update("INSERT INTO team_members(team_id,tournament_id,user_id) VALUES (?,?,?)",
                    id, tournamentId, userId);
            return id;
        }));
    }

    public void joinTeam(UUID tournamentId, UUID teamId, UUID userId) {
        transactions.executeWithoutResult(tx -> {
            requireRegistration(tournamentId);
            Integer count = jdbc.queryForObject("SELECT count(*) FROM team_members WHERE team_id=? AND tournament_id=?",
                    Integer.class, teamId, tournamentId);
            if (count == null || count == 0) throw new IllegalArgumentException("Team not found");
            if (count >= 3) throw new IllegalStateException("Team is full");
            jdbc.update("INSERT INTO team_members(team_id,tournament_id,user_id) VALUES (?,?,?)",
                    teamId, tournamentId, userId);
        });
    }

    public UUID submit(UUID tournamentId, UUID teamId, UUID userId, String repoUrl, String sha, String language) {
        if (repoUrl == null || !repoUrl.matches("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/?")) {
            throw new IllegalArgumentException("A public GitHub repository URL is required");
        }
        if (sha == null || !sha.matches("[0-9a-f]{40}")) throw new IllegalArgumentException("Full lowercase commit SHA required");
        if (!List.of("python", "cpp", "java").contains(language)) throw new IllegalArgumentException("Unsupported language");
        return Objects.requireNonNull(transactions.execute(tx -> {
            requireRegistration(tournamentId);
            Integer member = jdbc.queryForObject("SELECT count(*) FROM team_members " +
                    "WHERE team_id=? AND tournament_id=? AND user_id=?", Integer.class, teamId, tournamentId, userId);
            if (member == null || member != 1) throw new AccessDeniedException("Not a member of this team");
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO submissions(id,tournament_id,team_id,submitted_by,repo_url,commit_sha," +
                    "language,status,created_at) VALUES (?,?,?,?,?,?,?,'PENDING',?)",
                    id, tournamentId, teamId, userId, repoUrl, sha, language, Timestamp.from(Instant.now()));
            return id;
        }));
    }

    public void freeze(UUID tournamentId) {
        transactions.executeWithoutResult(tx -> {
            if (jdbc.update("UPDATE tournaments SET status='SUBMISSION_FROZEN' " +
                    "WHERE id=? AND status='REGISTRATION_OPEN'", tournamentId) != 1) {
                throw new IllegalStateException("Tournament cannot be frozen");
            }
            List<UUID> teams = jdbc.query("SELECT id FROM teams WHERE tournament_id=? ORDER BY name,id",
                    (rs, row) -> rs.getObject(1, UUID.class), tournamentId);
            int seed = 1;
            for (UUID team : teams) {
                List<UUID> submissions = jdbc.query("SELECT id FROM submissions WHERE team_id=? AND status='PASSED' " +
                        "AND artifact_url IS NOT NULL AND artifact_sha256 IS NOT NULL " +
                        "ORDER BY created_at DESC,id DESC LIMIT 1",
                        (rs, row) -> rs.getObject(1, UUID.class), team);
                if (!submissions.isEmpty()) {
                    jdbc.update("UPDATE teams SET seed=? WHERE id=?", seed, team);
                    jdbc.update("INSERT INTO tournament_entries(tournament_id,team_id,submission_id,seed) VALUES (?,?,?,?)",
                            tournamentId, team, submissions.get(0), seed++);
                }
            }
        });
    }

    private void requireRegistration(UUID tournamentId) {
        List<String> status = jdbc.query("SELECT status FROM tournaments WHERE id=? FOR UPDATE",
                (rs, row) -> rs.getString(1), tournamentId);
        if (status.isEmpty()) throw new IllegalArgumentException("Tournament not found");
        if (!"REGISTRATION_OPEN".equals(status.get(0))) throw new IllegalStateException("Submissions are frozen");
    }
}
