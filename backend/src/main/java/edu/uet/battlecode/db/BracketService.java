package edu.uet.battlecode.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public final class BracketService {
    private static final int[][] FIRST_ROUND_SEEDS = {
            {1,16}, {8,9}, {4,13}, {5,12}, {2,15}, {7,10}, {3,14}, {6,11}
    };
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    private record Entry(UUID teamId, UUID submissionId, int seed) {}
    private record Series(UUID id, UUID roundId, int position, UUID one, UUID two, String status) {}
    private record Game(int number, String status, UUID slotOne, UUID slotTwo, Integer winnerSlot) {
        UUID winner() { return winnerSlot == null ? null : winnerSlot == 1 ? slotOne : slotTwo; }
    }

    public BracketService(DataSource source) {
        jdbc = new JdbcTemplate(source);
        transactions = new TransactionTemplate(new JdbcTransactionManager(source));
    }

    public void generate(UUID tournamentId) {
        transactions.executeWithoutResult(tx -> {
            List<String> statuses = jdbc.query("SELECT status FROM tournaments WHERE id=? FOR UPDATE",
                    (rs, row) -> rs.getString(1), tournamentId);
            if (statuses.size() != 1 || !"SUBMISSION_FROZEN".equals(statuses.get(0))) {
                throw new IllegalStateException("Tournament must be frozen");
            }
            List<Entry> entries = jdbc.query("SELECT team_id,submission_id,seed FROM tournament_entries " +
                    "WHERE tournament_id=? ORDER BY seed", (rs, row) -> new Entry(
                    rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getInt(3)), tournamentId);
            if (entries.size() != 16) throw new IllegalStateException("Exactly 16 validated teams required");
            for (int i = 0; i < 16; i++) if (entries.get(i).seed() != i + 1) {
                throw new IllegalStateException("Seeds must be 1 through 16");
            }
            long tournamentSeed = tournamentSeed(tournamentId);
            List<List<UUID>> seriesByRound = new ArrayList<>();
            for (int round = 1; round <= 4; round++) {
                UUID roundId = UUID.randomUUID();
                jdbc.update("INSERT INTO rounds(id,tournament_id,round_number,status) VALUES (?,?,?,'PENDING')",
                        roundId, tournamentId, round);
                List<UUID> seriesIds = new ArrayList<>();
                for (int pos = 1; pos <= (16 >> round); pos++) {
                    UUID seriesId = UUID.randomUUID();
                    jdbc.update("INSERT INTO series(id,round_id,position,status) VALUES (?,?,?,'PENDING')",
                            seriesId, roundId, pos);
                    seriesIds.add(seriesId);
                }
                seriesByRound.add(seriesIds);
            }
            for (int pos = 0; pos < 8; pos++) {
                UUID seriesId = seriesByRound.get(0).get(pos);
                Entry one = entries.get(FIRST_ROUND_SEEDS[pos][0] - 1);
                Entry two = entries.get(FIRST_ROUND_SEEDS[pos][1] - 1);
                jdbc.update("UPDATE series SET team_one_id=?,team_two_id=?,status='RUNNING' WHERE id=?",
                        one.teamId(), two.teamId(), seriesId);
                enqueue(seriesId, one.teamId(), two.teamId(), one.submissionId(), two.submissionId(),
                        1, tournamentSeed ^ (1L << 32) ^ (pos + 1));
                enqueue(seriesId, two.teamId(), one.teamId(), two.submissionId(), one.submissionId(),
                        2, tournamentSeed ^ (1L << 32) ^ (pos + 1));
            }
            jdbc.update("UPDATE rounds SET status='RUNNING' WHERE tournament_id=? AND round_number=1", tournamentId);
            jdbc.update("UPDATE tournaments SET status='RUNNING_MATCHES' WHERE id=?", tournamentId);
        });
    }

    public void reconcile(UUID matchId) {
        transactions.executeWithoutResult(tx -> {
            List<UUID> seriesIds = jdbc.query("SELECT series_id FROM matches WHERE id=? AND series_id IS NOT NULL",
                    (rs, row) -> rs.getObject(1, UUID.class), matchId);
            if (seriesIds.isEmpty()) return;
            Series series = getSeriesForUpdate(seriesIds.get(0));
            if ("COMPLETED".equals(series.status())) return;
            List<Game> games = jdbc.query("SELECT game_number,status,team_one_id,team_two_id,winner_slot " +
                    "FROM matches WHERE series_id=? ORDER BY game_number", (rs, row) -> new Game(
                    rs.getInt(1), rs.getString(2), rs.getObject(3, UUID.class),
                    rs.getObject(4, UUID.class), (Integer) rs.getObject(5)), series.id());
            if (games.size() < 2 || !"COMPLETED".equals(games.get(0).status()) ||
                    !"COMPLETED".equals(games.get(1).status())) return;
            int oneWins = 0, twoWins = 0;
            for (Game game : games) if ("COMPLETED".equals(game.status())) {
                if (series.one().equals(game.winner())) oneWins++;
                if (series.two().equals(game.winner())) twoWins++;
            }
            if (games.size() == 2 && oneWins == twoWins) {
                UUID tournamentId = tournamentOfRound(series.roundId());
                long seed = tournamentSeed(tournamentId) ^ (roundNumber(series.roundId()) * 1L << 32) ^
                        series.position() ^ 0x5eedL;
                enqueue(series.id(), series.one(), series.two(), submission(tournamentId, series.one()),
                        submission(tournamentId, series.two()), 3, seed);
                return;
            }
            if (games.size() == 3 && !"COMPLETED".equals(games.get(2).status())) return;
            UUID winner = oneWins > twoWins ? series.one() : twoWins > oneWins ? series.two()
                    : higherSeed(tournamentOfRound(series.roundId()), series.one(), series.two());
            jdbc.update("UPDATE series SET status='COMPLETED',winner_team_id=? WHERE id=?", winner, series.id());
            advance(series, winner);
        });
    }

    private void advance(Series series, UUID winner) {
        int round = roundNumber(series.roundId());
        UUID tournamentId = tournamentOfRound(series.roundId());
        if (round == 4) {
            jdbc.update("UPDATE rounds SET status='COMPLETED' WHERE id=?", series.roundId());
            jdbc.update("UPDATE tournaments SET status='COMPLETED' WHERE id=?", tournamentId);
            return;
        }
        UUID nextRoundId = jdbc.queryForObject("SELECT id FROM rounds WHERE tournament_id=? AND round_number=?",
                UUID.class, tournamentId, round + 1);
        UUID nextSeriesId = jdbc.queryForObject("SELECT id FROM series WHERE round_id=? AND position=?",
                UUID.class, nextRoundId, (series.position() + 1) / 2);
        Series next = getSeriesForUpdate(nextSeriesId);
        if (series.position() % 2 == 1) {
            if (next.one() != null && !next.one().equals(winner)) throw new IllegalStateException("Bracket slot already assigned");
            jdbc.update("UPDATE series SET team_one_id=? WHERE id=?", winner, nextSeriesId);
        } else {
            if (next.two() != null && !next.two().equals(winner)) throw new IllegalStateException("Bracket slot already assigned");
            jdbc.update("UPDATE series SET team_two_id=? WHERE id=?", winner, nextSeriesId);
        }
        next = getSeriesForUpdate(nextSeriesId);
        if (next.one() != null && next.two() != null && "PENDING".equals(next.status())) {
            long seed = tournamentSeed(tournamentId) ^ ((round + 1L) << 32) ^ next.position();
            enqueue(next.id(), next.one(), next.two(), submission(tournamentId, next.one()),
                    submission(tournamentId, next.two()), 1, seed);
            enqueue(next.id(), next.two(), next.one(), submission(tournamentId, next.two()),
                    submission(tournamentId, next.one()), 2, seed);
            jdbc.update("UPDATE series SET status='RUNNING' WHERE id=?", next.id());
            jdbc.update("UPDATE rounds SET status='RUNNING' WHERE id=?", next.roundId());
        }
        Integer remaining = jdbc.queryForObject("SELECT count(*) FROM series WHERE round_id=? AND status<>'COMPLETED'",
                Integer.class, series.roundId());
        if (remaining != null && remaining == 0) {
            jdbc.update("UPDATE rounds SET status='COMPLETED' WHERE id=?", series.roundId());
        }
    }

    public boolean publish(UUID tournamentId) {
        return jdbc.update("UPDATE tournaments SET status='PUBLISHED' WHERE id=? AND status='COMPLETED'",
                tournamentId) == 1;
    }

    private void enqueue(UUID seriesId, UUID one, UUID two, UUID submissionOne, UUID submissionTwo,
                         int gameNumber, long seed) {
        jdbc.update("INSERT INTO matches(id,series_id,game_number,team_one_id,team_two_id," +
                "submission_one_id,submission_two_id,random_seed,status) " +
                "VALUES (?,?,?,?,?,?,?,?,'QUEUED')", UUID.randomUUID(), seriesId, gameNumber,
                one, two, submissionOne, submissionTwo, seed);
    }

    private Series getSeriesForUpdate(UUID id) {
        return jdbc.queryForObject("SELECT id,round_id,position,team_one_id,team_two_id,status " +
                "FROM series WHERE id=? FOR UPDATE", (rs, row) -> new Series(rs.getObject(1, UUID.class),
                rs.getObject(2, UUID.class), rs.getInt(3), rs.getObject(4, UUID.class),
                rs.getObject(5, UUID.class), rs.getString(6)), id);
    }

    private UUID tournamentOfRound(UUID roundId) {
        return jdbc.queryForObject("SELECT tournament_id FROM rounds WHERE id=?", UUID.class, roundId);
    }

    private int roundNumber(UUID roundId) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT round_number FROM rounds WHERE id=?", Integer.class, roundId));
    }

    private long tournamentSeed(UUID tournamentId) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT seed FROM tournaments WHERE id=?", Long.class, tournamentId));
    }

    private UUID submission(UUID tournamentId, UUID teamId) {
        return jdbc.queryForObject("SELECT submission_id FROM tournament_entries WHERE tournament_id=? AND team_id=?",
                UUID.class, tournamentId, teamId);
    }

    private UUID higherSeed(UUID tournamentId, UUID one, UUID two) {
        Integer first = jdbc.queryForObject("SELECT seed FROM tournament_entries WHERE tournament_id=? AND team_id=?",
                Integer.class, tournamentId, one);
        Integer second = jdbc.queryForObject("SELECT seed FROM tournament_entries WHERE tournament_id=? AND team_id=?",
                Integer.class, tournamentId, two);
        return first < second ? one : two;
    }
}
