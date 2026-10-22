package edu.uet.battlecode.replay;

import static org.junit.jupiter.api.Assertions.*;

import edu.uet.battlecode.game.BotAction;
import edu.uet.battlecode.game.CyberGridEngine;
import edu.uet.battlecode.game.Direction;
import edu.uet.battlecode.game.GameConfig;
import edu.uet.battlecode.game.MatchState;
import edu.uet.battlecode.game.TeamInfo;
import edu.uet.battlecode.game.TickResult;
import edu.uet.battlecode.game.UnitType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReplayWriterTest {
    @Test void everyDeltaReconstructsExactStateAndTickFiftyIsKeyframe() throws Exception {
        CyberGridEngine engine = new CyberGridEngine();
        List<TeamInfo> teams = List.of(new TeamInfo(1, "A"), new TeamInfo(2, "B"));
        MatchState state = engine.init(new GameConfig(16, 52, true, 10, 5), teams, 41L);
        ReplayWriter writer = new ReplayWriter("match-1", state, teams, engine);
        for (int tick = 1; tick <= 52; tick++) {
            Map<Integer, List<BotAction>> actions = tick == 1
                    ? Map.of(1, List.of(BotAction.spawn(state.entities(1, UnitType.CORE).get(0).id(),
                            Direction.SOUTH, UnitType.PAWN))) : Map.of();
            TickResult result = engine.step(state, actions);
            writer.append(state, result);
        }
        byte[] gzip = writer.toGzip();
        assertEquals(0x1f, gzip[0] & 0xff);
        assertEquals(0x8b, gzip[1] & 0xff);
        ReplayReader reader = new ReplayReader(gzip);
        assertEquals(52, reader.frames().size());
        assertNotNull(reader.frames().get(49).keyframe());
        assertEquals(ReplayState.capture(state, engine), reader.reconstruct(52));
        assertEquals(0, reader.reconstruct(0).tick());
        assertEquals(50, reader.reconstruct(50).tick());
    }
}
