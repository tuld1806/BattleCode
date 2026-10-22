package edu.uet.battlecode.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class CyberGridEngine {
    public MatchState init(GameConfig config, List<TeamInfo> teams, long seed) {
        if (teams.size() != 2 || teams.get(0).slot() != 1 || teams.get(1).slot() != 2) {
            throw new IllegalArgumentException("Expected team slots 1 and 2");
        }
        int size = config.size();
        boolean[][] obstacles = new boolean[size][size];
        boolean[][] nodes = new boolean[size][size];
        Random random = new Random(seed);
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            if (y * size + x >= (size * size + 1) / 2) continue;
            int rx = size - 1 - x, ry = size - 1 - y;
            boolean protectedZone = nearStart(x, y, size) || nearStart(rx, ry, size);
            boolean obstacle = !protectedZone && random.nextDouble() < 0.11;
            boolean node = !protectedZone && !obstacle && random.nextDouble() < 0.07;
            obstacles[y][x] = obstacles[ry][rx] = obstacle;
            nodes[y][x] = nodes[ry][rx] = node;
        }
        MatchState state = new MatchState(config, seed, obstacles, nodes);
        state.add(1, UnitType.CORE, 1, 1, 0);
        state.add(1, UnitType.WORKER, 2, 1, 0);
        state.add(2, UnitType.CORE, size - 2, size - 2, 0);
        state.add(2, UnitType.WORKER, size - 3, size - 2, 0);
        return state;
    }

    private boolean nearStart(int x, int y, int size) {
        return Math.abs(x - 1) + Math.abs(y - 1) <= 3 ||
                Math.abs(x - (size - 2)) + Math.abs(y - (size - 2)) <= 3;
    }

    public Observation observe(MatchState state, int team) {
        if (team != 1 && team != 2) throw new IllegalArgumentException("Invalid team slot");
        int size = state.config().size();
        boolean[][] visible = new boolean[size][size];
        for (Entity entity : state.entities()) if (entity.team() == team && entity.hp() > 0) {
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                if (!state.config().fogOfWar() || distance(entity.x(), entity.y(), x, y) <= entity.type().vision) {
                    visible[y][x] = true;
                }
            }
        }
        List<VisibleCell> cells = new ArrayList<>();
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            if (visible[y][x]) cells.add(new VisibleCell(x, y, state.isObstacle(x, y), state.isResourceNode(x, y)));
        }
        List<UnitView> friends = new ArrayList<>(), enemies = new ArrayList<>();
        for (Entity entity : state.entities()) if (entity.hp() > 0) {
            if (entity.team() == team) friends.add(UnitView.of(entity));
            else if (visible[entity.y()][entity.x()]) enemies.add(UnitView.of(entity));
        }
        return new Observation(state.tick(), state.seed(), team, size, state.resources(team),
                List.copyOf(cells), List.copyOf(friends), List.copyOf(enemies));
    }

    public TickResult step(MatchState state, Map<Integer, List<BotAction>> proposed) {
        if (isTerminal(state)) throw new IllegalStateException("Match already ended");
        List<String> events = new ArrayList<>();
        Map<Integer, BotAction> actions = canonicalActions(state, proposed, events);
        List<Entity> active = state.entities().stream().filter(e -> e.hp() > 0 && e.activeFromTick() <= state.tick()).toList();

        // Phase 1: all new units are inactive until the next tick.
        for (Entity actor : active) {
            BotAction action = actions.get(actor.id());
            if (action.type() != ActionType.SPAWN && action.type() != ActionType.BUILD) continue;
            UnitType created = action.type() == ActionType.BUILD ? UnitType.TOWER : action.unitType();
            if (action.direction() == null || created == null ||
                    (action.type() == ActionType.SPAWN && (actor.type() != UnitType.CORE || !created.mobile)) ||
                    (action.type() == ActionType.BUILD && actor.type() != UnitType.WORKER)) continue;
            int x = actor.x() + action.direction().dx, y = actor.y() + action.direction().dy;
            if (!state.inBounds(x, y) || state.isObstacle(x, y) || state.occupied(x, y) ||
                    state.resources(actor.team()) < created.cost) continue;
            if (created.mobile && state.entities().stream().filter(e -> e.team() == actor.team() && e.type().mobile && e.hp() > 0).count() >= 60) continue;
            state.spend(actor.team(), created.cost);
            Entity spawned = state.add(actor.team(), created, x, y, state.tick() + 1);
            events.add("SPAWN " + actor.id() + " " + spawned.id());
        }

        // Phase 2: reject blocked destinations, contested destinations, occupied cells and swaps.
        Map<Integer, int[]> wants = new HashMap<>();
        for (Entity actor : active) {
            BotAction action = actions.get(actor.id());
            if (action.type() != ActionType.MOVE || !actor.type().mobile || action.direction() == null) continue;
            int x = actor.x() + action.direction().dx, y = actor.y() + action.direction().dy;
            if (!state.inBounds(x, y) || state.isObstacle(x, y)) {
                actor.damage(10); events.add("COLLISION " + actor.id());
            } else wants.put(actor.id(), new int[] {x, y});
        }
        Set<Integer> bounced = new HashSet<>();
        for (var entry : wants.entrySet()) {
            Entity actor = state.entity(entry.getKey());
            int[] dest = entry.getValue();
            for (Entity other : state.entities()) {
                if (other.id() != actor.id() && other.hp() > 0 && other.x() == dest[0] && other.y() == dest[1]) {
                    bounced.add(actor.id());
                    if (wants.containsKey(other.id())) bounced.add(other.id());
                }
            }
            for (var rival : wants.entrySet()) {
                if (rival.getKey() != actor.id() && rival.getValue()[0] == dest[0] && rival.getValue()[1] == dest[1]) {
                    bounced.add(actor.id()); bounced.add(rival.getKey());
                }
            }
        }
        for (Entity actor : active) if (wants.containsKey(actor.id())) {
            if (bounced.contains(actor.id())) { actor.damage(10); events.add("COLLISION " + actor.id()); }
            else { int[] dest = wants.get(actor.id()); actor.moveTo(dest[0], dest[1]); events.add("MOVE " + actor.id()); }
        }
        state.removeDead();

        // Phase 3: take a snapshot of attacks, then apply damage together.
        Map<Integer, Integer> damage = new HashMap<>();
        for (Entity actor : active) {
            if (actor.hp() <= 0) continue;
            BotAction action = actions.get(actor.id());
            if (action.type() != ActionType.ATTACK || action.targetId() == null) continue;
            Entity target = state.entity(action.targetId());
            if (target == null || target.hp() <= 0 || target.team() == actor.team() ||
                    distance(actor.x(), actor.y(), target.x(), target.y()) > actor.type().range ||
                    !lineClear(state, actor.x(), actor.y(), target.x(), target.y())) continue;
            damage.merge(target.id(), actor.type().attack, Integer::sum);
            events.add("ATTACK " + actor.id() + " " + target.id());
        }
        damage.forEach(state::damage);
        state.removeDead();

        // Phase 4: core income and stationary worker mining.
        for (int team = 1; team <= 2; team++) {
            if (!state.entities(team, UnitType.CORE).isEmpty() && state.entities(team, UnitType.CORE).get(0).hp() > 0) {
                state.earn(team, state.config().passiveIncome());
            }
            for (Entity worker : state.entities(team, UnitType.WORKER)) {
                if (worker.hp() > 0 && state.isResourceNode(worker.x(), worker.y())) state.earn(team, state.config().mineIncome());
            }
        }
        state.advance();
        return new TickResult(state.tick(), List.copyOf(events), isTerminal(state));
    }

    private Map<Integer, BotAction> canonicalActions(MatchState state, Map<Integer, List<BotAction>> proposed, List<String> events) {
        Map<Integer, BotAction> actions = new HashMap<>();
        for (Entity entity : state.entities()) {
            if (entity.hp() > 0 && entity.activeFromTick() <= state.tick()) {
                actions.put(entity.id(), BotAction.waitFor(entity.id()));
            }
        }
        Set<Integer> invalid = new HashSet<>();
        if (proposed == null) proposed = Map.of();
        for (int team = 1; team <= 2; team++) {
            Set<Integer> seen = new HashSet<>();
            List<BotAction> list = proposed.get(team);
            if (list == null) continue;
            for (BotAction action : list) {
                if (action == null) { events.add("INVALID null"); continue; }
                Entity entity = state.entity(action.unitId());
                if (entity == null || entity.team() != team || !actions.containsKey(action.unitId()) ||
                        !seen.add(action.unitId()) || action.type() == null) {
                    events.add("INVALID " + action.unitId());
                    if (entity != null && entity.team() == team && actions.containsKey(action.unitId())) {
                        invalid.add(action.unitId());
                        actions.put(action.unitId(), BotAction.waitFor(action.unitId()));
                    }
                    continue;
                }
                if (!invalid.contains(action.unitId())) actions.put(action.unitId(), action);
            }
        }
        for (Entity entity : state.entities()) {
            BotAction action = actions.get(entity.id());
            if (action != null && action.type() == ActionType.WAIT) events.add("WAIT " + entity.id());
        }
        return actions;
    }

    public boolean isTerminal(MatchState state) {
        return state.tick() >= state.config().maxTicks() ||
                state.entities(1, UnitType.CORE).stream().noneMatch(e -> e.hp() > 0) ||
                state.entities(2, UnitType.CORE).stream().noneMatch(e -> e.hp() > 0);
    }

    public MatchResult results(MatchState state) {
        if (!isTerminal(state)) throw new IllegalStateException("Match still in progress");
        int comparison = Integer.compare(coreHp(state, 1), coreHp(state, 2));
        if (comparison == 0) comparison = Integer.compare(totalHp(state, 1), totalHp(state, 2));
        if (comparison == 0) comparison = Integer.compare(state.resources(1), state.resources(2));
        return new MatchResult(comparison == 0 ? null : comparison > 0 ? 1 : 2, comparison == 0 ? "DRAW" : "SCORE");
    }

    private int coreHp(MatchState state, int team) { return state.entities(team, UnitType.CORE).stream().mapToInt(Entity::hp).sum(); }
    private int totalHp(MatchState state, int team) { return state.entities().stream().filter(e -> e.team() == team).mapToInt(Entity::hp).sum(); }
    private int distance(int x, int y, int xx, int yy) { return Math.abs(x - xx) + Math.abs(y - yy); }
    private boolean lineClear(MatchState state, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (x0 != x1 || y0 != y1) {
            int doubled = 2 * err;
            if (doubled > -dy) { err -= dy; x0 += sx; }
            if (doubled < dx) { err += dx; y0 += sy; }
            if ((x0 != x1 || y0 != y1) && state.isObstacle(x0, y0)) return false;
        }
        return true;
    }
}
