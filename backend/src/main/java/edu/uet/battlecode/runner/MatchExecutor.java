package edu.uet.battlecode.runner;

import edu.uet.battlecode.game.BotAction;
import edu.uet.battlecode.game.CyberGridEngine;
import edu.uet.battlecode.game.MatchResult;
import edu.uet.battlecode.game.MatchState;
import edu.uet.battlecode.game.Observation;
import edu.uet.battlecode.game.TickResult;
import edu.uet.battlecode.replay.ReplayWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class MatchExecutor {
    private final CyberGridEngine engine = new CyberGridEngine();

    public MatchOutcome execute(MatchSpec spec) throws BotProtocolException, IOException {
        try (BotSession one = BotSession.start(spec.botOne(), spec.sandboxOne());
             BotSession two = BotSession.start(spec.botTwo(), spec.sandboxTwo())) {
            return executeWithSessions(spec, one, two);
        }
    }

    MatchOutcome executeWithSessions(MatchSpec spec, BotSession one, BotSession two) throws IOException {
        MatchState state = engine.init(spec.config(), spec.teams(), spec.seed());
        ReplayWriter replay = new ReplayWriter(spec.matchId(), state, spec.teams(), engine);
        try (ExecutorService requests = Executors.newVirtualThreadPerTaskExecutor()) {
            while (!engine.isTerminal(state)) {
                Observation first = engine.observe(state, 1);
                Observation second = engine.observe(state, 2);
                Future<BotTurnResult> firstTask = requests.submit(() -> one.request(first));
                Future<BotTurnResult> secondTask = requests.submit(() -> two.request(second));
                BotTurnResult a = await(firstTask, one);
                BotTurnResult b = await(secondTask, two);
                if (a.disqualified() || b.disqualified()) {
                    Integer winner = a.disqualified() && b.disqualified() ? null : a.disqualified() ? 2 : 1;
                    String reason = a.disqualified() && b.disqualified() ? "BOTH_DISQUALIFIED" :
                            a.disqualified() ? a.error() : b.error();
                    return new MatchOutcome(new MatchResult(winner, reason), replay.toGzip(), state.tick());
                }
                Map<Integer, List<BotAction>> actions = Map.of(1, a.actions(), 2, b.actions());
                TickResult tick = engine.step(state, actions);
                replay.append(state, tick, actions, Map.of(1, first, 2, second),
                        Map.of(1, a.debugLogs(), 2, b.debugLogs()));
            }
        }
        return new MatchOutcome(engine.results(state), replay.toGzip(), state.tick());
    }

    private BotTurnResult await(Future<BotTurnResult> task, BotSession bot) {
        try {
            return task.get(2_250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            bot.close();
            return new BotTurnResult(List.of(), "DISQUALIFIED_INTERRUPTED", true, 0, 0, "");
        } catch (ExecutionException | TimeoutException e) {
            task.cancel(true);
            bot.close();
            return new BotTurnResult(List.of(), "DISQUALIFIED_TIMEOUT", true, 0, 0, "");
        }
    }
}
