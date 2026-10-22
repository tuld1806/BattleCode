package edu.uet.battlecode.runner;

import static org.junit.jupiter.api.Assertions.*;

import edu.uet.battlecode.game.GameConfig;
import edu.uet.battlecode.game.TeamInfo;
import edu.uet.battlecode.replay.ReplayReader;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchExecutorTest {
    private BotSession bot(String mode, long bankMs) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        return BotSession.startTrustedForTest(List.of(java, "-cp", System.getProperty("java.class.path"),
                TestBotMain.class.getName(), mode), bankMs);
    }

    @Test void validBotsFinishAndProduceReplay() throws Exception {
        MatchSpec spec = new MatchSpec("test", new GameConfig(16, 2, true, 10, 5),
                List.of(new TeamInfo(1, "A"), new TeamInfo(2, "B")), 41L,
                null, null, null, null);
        try (BotSession a = bot("valid", 2_000); BotSession b = bot("valid", 2_000)) {
            MatchOutcome outcome = new MatchExecutor().executeWithSessions(spec, a, b);
            assertEquals(2, outcome.ticks());
            assertEquals(2, new ReplayReader(outcome.replayGzip()).frames().size());
            assertEquals("DRAW", outcome.result().reason());
        }
    }

    @Test void timedOutBotLosesWithoutBlockingOtherBot() throws Exception {
        MatchSpec spec = new MatchSpec("test", new GameConfig(16, 2, true, 10, 5),
                List.of(new TeamInfo(1, "A"), new TeamInfo(2, "B")), 41L,
                null, null, null, null);
        try (BotSession a = bot("timeout", 0); BotSession b = bot("valid", 2_000)) {
            MatchOutcome outcome = new MatchExecutor().executeWithSessions(spec, a, b);
            assertEquals(2, outcome.result().winnerSlot());
            assertEquals("DISQUALIFIED_TIMEOUT", outcome.result().reason());
        }
    }
}
