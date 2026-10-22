package edu.uet.battlecode.db;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;

class TournamentServiceTest {
    private TournamentService tournaments;
    private JdbcTemplate jdbc;
    private UUID tournamentId, captainId, outsiderId, teamId;
    private static final String SHA_ONE = "a".repeat(40);
    private static final String SHA_TWO = "b".repeat(40);

    @BeforeEach void setUp() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:tournament" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE profiles(id UUID PRIMARY KEY, role VARCHAR(16))");
        jdbc.execute("CREATE TABLE tournaments(id UUID PRIMARY KEY, status VARCHAR(24), seed BIGINT)");
        jdbc.execute("CREATE TABLE teams(id UUID PRIMARY KEY, tournament_id UUID, name VARCHAR(100), captain_id UUID, seed INT)");
        jdbc.execute("CREATE TABLE team_members(team_id UUID, tournament_id UUID, user_id UUID)");
        jdbc.execute("CREATE TABLE submissions(id UUID PRIMARY KEY, tournament_id UUID, team_id UUID, submitted_by UUID, " +
                "repo_url VARCHAR(500), commit_sha CHAR(40), language VARCHAR(8), status VARCHAR(16), " +
                "artifact_url VARCHAR(1000), artifact_sha256 CHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE tournament_entries(tournament_id UUID, team_id UUID, submission_id UUID, seed INT)");
        tournamentId = UUID.randomUUID(); captainId = UUID.randomUUID(); outsiderId = UUID.randomUUID();
        jdbc.update("INSERT INTO profiles VALUES (?, 'STUDENT')", captainId);
        jdbc.update("INSERT INTO profiles VALUES (?, 'STUDENT')", outsiderId);
        jdbc.update("INSERT INTO tournaments VALUES (?, 'REGISTRATION_OPEN', 42)", tournamentId);
        tournaments = new TournamentService(source);
        teamId = tournaments.registerTeam(tournamentId, captainId, "Alpha");
    }

    @Test void onlyTeamMemberCanSubmitAndCommitIsNewRow() {
        assertThrows(AccessDeniedException.class, () -> tournaments.submit(tournamentId, teamId,
                outsiderId, "https://github.com/u/repo", SHA_ONE, "python"));
        UUID first = tournaments.submit(tournamentId, teamId, captainId,
                "https://github.com/u/repo", SHA_ONE, "python");
        UUID second = tournaments.submit(tournamentId, teamId, captainId,
                "https://github.com/u/repo", SHA_TWO, "cpp");
        assertNotEquals(first, second);
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM submissions", Integer.class));
    }

    @Test void freezePinsLatestPassedShaAndRejectsNewSubmissions() {
        UUID old = tournaments.submit(tournamentId, teamId, captainId,
                "https://github.com/u/repo", SHA_ONE, "python");
        UUID newest = tournaments.submit(tournamentId, teamId, captainId,
                "https://github.com/u/repo", SHA_TWO, "python");
        jdbc.update("UPDATE submissions SET status='PASSED', artifact_url='https://r2.example/a', artifact_sha256=? WHERE id=?",
                "c".repeat(64), old);
        jdbc.update("UPDATE submissions SET status='PASSED', artifact_url='https://r2.example/b', artifact_sha256=? WHERE id=?",
                "d".repeat(64), newest);
        tournaments.freeze(tournamentId);
        assertEquals(newest, jdbc.queryForObject("SELECT submission_id FROM tournament_entries WHERE team_id=?",
                UUID.class, teamId));
        assertThrows(IllegalStateException.class, () -> tournaments.submit(tournamentId, teamId,
                captainId, "https://github.com/u/repo", "c".repeat(40), "java"));
    }
}
