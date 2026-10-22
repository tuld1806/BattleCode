package edu.uet.battlecode.runner;

import static org.junit.jupiter.api.Assertions.*;

import edu.uet.battlecode.game.CyberGridEngine;
import edu.uet.battlecode.game.GameConfig;
import edu.uet.battlecode.game.Observation;
import edu.uet.battlecode.game.TeamInfo;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BotSessionTest {
    private final Observation observation = new CyberGridEngine().observe(
            new CyberGridEngine().init(new GameConfig(16, 200, true, 10, 5),
                    List.of(new TeamInfo(1, "A"), new TeamInfo(2, "B")), 1), 1);

    private BotSession start(String mode, long bankMs) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        return BotSession.startTrustedForTest(List.of(java, "-cp", System.getProperty("java.class.path"),
                TestBotMain.class.getName(), mode), bankMs);
    }

    @Test void persistentBotCanAnswerMoreThanOneTurn() throws Exception {
        try (BotSession bot = start("valid", 2_000)) {
            assertEquals(1, bot.request(observation).actions().size());
            assertEquals(1, bot.request(observation).actions().size());
        }
    }

    @Test void malformedJsonRecordsWaitWithoutCrashing() throws Exception {
        try (BotSession bot = start("malformed", 2_000)) {
            BotTurnResult turn = bot.request(observation);
            assertTrue(turn.actions().isEmpty());
            assertEquals("MALFORMED_ACTION", turn.error());
            assertFalse(turn.disqualified());
        }
    }

    @Test void timeoutKillsBot() throws Exception {
        try (BotSession bot = start("timeout", 0)) {
            assertEquals("DISQUALIFIED_TIMEOUT", bot.request(observation).error());
            assertFalse(bot.isAlive());
        }
    }

    @Test void oversizedOutputKillsBot() throws Exception {
        try (BotSession bot = start("oversized", 2_000)) {
            assertEquals("DISQUALIFIED_OUTPUT", bot.request(observation).error());
            assertFalse(bot.isAlive());
        }
    }

    @Test void stderrIsDrainedAndCapped() throws Exception {
        try (BotSession bot = start("stderr", 2_000)) {
            BotTurnResult turn = bot.request(observation);
            assertFalse(turn.disqualified());
            assertTrue(turn.debugLogs().length() <= 200);
            assertEquals(1, turn.actions().size());
        }
    }

    @Test void missingReadyFailsStartup() {
        assertThrows(BotProtocolException.class, () -> start("no-ready", 2_000));
    }
}
