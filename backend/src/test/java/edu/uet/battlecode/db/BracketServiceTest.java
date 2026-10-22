package edu.uet.battlecode.db;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class BracketServiceTest {
    private JdbcTemplate jdbc;
    private BracketService bracket;
    private UUID tournamentId;

    @BeforeEach void setUp() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:bracket" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE tournaments(id UUID PRIMARY KEY,status VARCHAR(24),seed BIGINT)");
        jdbc.execute("CREATE TABLE rounds(id UUID PRIMARY KEY,tournament_id UUID,round_number INT,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE series(id UUID PRIMARY KEY,round_id UUID,position INT,team_one_id UUID," +
                "team_two_id UUID,winner_team_id UUID,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE tournament_entries(tournament_id UUID,team_id UUID,submission_id UUID,seed INT)");
        jdbc.execute("CREATE TABLE matches(id UUID PRIMARY KEY,series_id UUID,game_number INT,team_one_id UUID," +
                "team_two_id UUID,submission_one_id UUID,submission_two_id UUID,random_seed BIGINT," +
                "status VARCHAR(16),winner_slot INT)");
        tournamentId = UUID.randomUUID();
        jdbc.update("INSERT INTO tournaments VALUES (?, 'SUBMISSION_FROZEN', 41)", tournamentId);
        for (int seed = 1; seed <= 16; seed++) {
            jdbc.update("INSERT INTO tournament_entries VALUES (?,?,?,?)",
                    tournamentId, UUID.randomUUID(), UUID.randomUUID(), seed);
        }
        bracket = new BracketService(source);
    }

    @Test void generatesMirroredFirstRoundAndAdvancesWinnerOnce() {
        bracket.generate(tournamentId);
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM rounds", Integer.class));
        assertEquals(15, jdbc.queryForObject("SELECT count(*) FROM series", Integer.class));
        assertEquals(16, jdbc.queryForObject("SELECT count(*) FROM matches", Integer.class));
        UUID seriesId = jdbc.queryForObject("SELECT s.id FROM series s JOIN rounds r ON r.id=s.round_id " +
                "WHERE r.round_number=1 AND s.position=1", UUID.class);
        List<UUID> matches = jdbc.query("SELECT id FROM matches WHERE series_id=? ORDER BY game_number",
                (rs, row) -> rs.getObject(1, UUID.class), seriesId);
        UUID firstSlotOne = jdbc.queryForObject("SELECT team_one_id FROM matches WHERE id=?", UUID.class, matches.get(0));
        UUID secondSlotTwo = jdbc.queryForObject("SELECT team_two_id FROM matches WHERE id=?", UUID.class, matches.get(1));
        assertEquals(firstSlotOne, secondSlotTwo);
        jdbc.update("UPDATE matches SET status='COMPLETED', winner_slot=1 WHERE id=?", matches.get(0));
        jdbc.update("UPDATE matches SET status='COMPLETED', winner_slot=2 WHERE id=?", matches.get(1));
        bracket.reconcile(matches.get(1));
        assertEquals(firstSlotOne, jdbc.queryForObject("SELECT winner_team_id FROM series WHERE id=?",
                UUID.class, seriesId));
        bracket.reconcile(matches.get(1));
        assertEquals(16, jdbc.queryForObject("SELECT count(*) FROM matches", Integer.class));
    }
}
