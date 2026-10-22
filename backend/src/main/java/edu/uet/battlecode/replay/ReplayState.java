package edu.uet.battlecode.replay;

import edu.uet.battlecode.game.CyberGridEngine;
import edu.uet.battlecode.game.Entity;
import edu.uet.battlecode.game.MatchState;
import edu.uet.battlecode.game.UnitType;
import edu.uet.battlecode.game.VisibleCell;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record ReplayState(int tick, Map<Integer, Unit> units, Map<Integer, Integer> resources,
                          Map<Integer, List<Integer>> visibility) {
    public record Unit(int id, int team, UnitType type, int x, int y, int hp) {
        static Unit from(Entity entity) {
            return new Unit(entity.id(), entity.team(), entity.type(), entity.x(), entity.y(), entity.hp());
        }
    }

    public ReplayState {
        units = Map.copyOf(units);
        resources = Map.copyOf(resources);
        visibility = Map.copyOf(visibility);
    }

    public static ReplayState capture(MatchState state, CyberGridEngine engine) {
        Map<Integer, Unit> units = new TreeMap<>();
        for (Entity entity : state.entities()) units.put(entity.id(), Unit.from(entity));
        Map<Integer, List<Integer>> visibility = new TreeMap<>();
        for (int team = 1; team <= 2; team++) {
            List<Integer> cells = new ArrayList<>();
            for (VisibleCell cell : engine.observe(state, team).visibleCells()) {
                cells.add(cell.y() * state.config().size() + cell.x());
            }
            visibility.put(team, List.copyOf(cells));
        }
        return new ReplayState(state.tick(), units,
                Map.of(1, state.resources(1), 2, state.resources(2)), visibility);
    }

    public ReplayState apply(ReplayDelta delta) {
        Map<Integer, Unit> next = new TreeMap<>(units);
        for (int id : delta.removed()) next.remove(id);
        next.putAll(delta.upserts());
        return new ReplayState(delta.tick(), next, delta.resources(), delta.visibility());
    }
}
