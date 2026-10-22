package edu.uet.battlecode.runner;

import edu.uet.battlecode.game.BotAction;
import java.util.List;

public record BotTurnResult(List<BotAction> actions, String error, boolean disqualified,
                            long elapsedNanos, long timeBankMs, String debugLogs) {
    public BotTurnResult {
        actions = List.copyOf(actions);
    }
}
