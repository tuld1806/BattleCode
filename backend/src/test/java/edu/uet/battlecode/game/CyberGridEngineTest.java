package edu.uet.battlecode.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CyberGridEngineTest {
    private final CyberGridEngine engine = new CyberGridEngine();
    private final GameConfig config = new GameConfig(16, 200, true, 10, 5);
    private final List<TeamInfo> teams = List.of(new TeamInfo(1, "A"), new TeamInfo(2, "B"));

    @Test void seededMapIsSymmetricAndRepeatable() {
        MatchState first = engine.init(config, teams, 1234L);
        MatchState second = engine.init(config, teams, 1234L);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            assertEquals(first.isObstacle(x, y), second.isObstacle(x, y));
            assertEquals(first.isObstacle(x, y), first.isObstacle(15 - x, 15 - y));
            assertEquals(first.isResourceNode(x, y), first.isResourceNode(15 - x, 15 - y));
        }
    }

    @Test void startsEachTeamWithCoreWorkerAndOneHundredResources() {
        MatchState state = engine.init(config, teams, 1L);
        assertEquals(100, state.resources(1));
        assertEquals(100, state.resources(2));
        assertEquals(1, state.entities(1, UnitType.CORE).size());
        assertEquals(1, state.entities(2, UnitType.CORE).size());
        assertEquals(1, state.entities(1, UnitType.WORKER).size());
        assertEquals(1, state.entities(2, UnitType.WORKER).size());
    }

    @Test void fogDoesNotRevealEnemyAtOppositeCorner() {
        MatchState state = engine.init(config, teams, 1L);
        Observation observation = engine.observe(state, 1);
        assertTrue(observation.visibleEnemies().isEmpty());
        assertTrue(observation.friendlyUnits().stream().anyMatch(u -> u.type() == UnitType.CORE));
        assertTrue(observation.visibleCells().stream().noneMatch(c -> c.x() >= 12 && c.y() >= 12));
    }

    @Test void passiveIncomeAppliesAfterEveryTick() {
        MatchState state = engine.init(config, teams, 1L);
        engine.step(state, Map.of());
        assertEquals(110, state.resources(1));
        assertEquals(110, state.resources(2));
        assertEquals(1, state.tick());
    }

    @Test void walkingIntoOwnCoreBouncesAndCostsTenHp() {
        MatchState state = engine.init(config, teams, 1L);
        Entity worker = state.entities(1, UnitType.WORKER).get(0);
        engine.step(state, Map.of(1, List.of(BotAction.move(worker.id(), Direction.WEST))));
        assertEquals(2, worker.x());
        assertEquals(1, worker.y());
        assertEquals(40, worker.hp());
    }

    @Test void bothDestroyedCoresDrawWhenAllTieBreakValuesEqual() {
        MatchState state = engine.init(config, teams, 1L);
        for (Entity core : state.entities(UnitType.CORE)) state.damage(core.id(), core.hp());
        assertTrue(engine.isTerminal(state));
        assertNull(engine.results(state).winnerSlot());
        assertEquals("DRAW", engine.results(state).reason());
    }

    @Test void competingMovesBounceBothMovers() {
        MatchState state = engine.init(config, teams, 1L);
        Entity a = state.add(1, UnitType.PAWN, 5, 5, 0);
        Entity b = state.add(2, UnitType.PAWN, 7, 5, 0);
        engine.step(state, Map.of(1, List.of(BotAction.move(a.id(), Direction.EAST)),
                2, List.of(BotAction.move(b.id(), Direction.WEST))));
        assertEquals(5, a.x());
        assertEquals(7, b.x());
        assertEquals(110, a.hp());
        assertEquals(110, b.hp());
    }

    @Test void simultaneousCombatLetsLethallyHitUnitsFire() {
        MatchState state = engine.init(config, teams, 1L);
        Entity a = state.add(1, UnitType.BLASTER, 5, 5, 0);
        Entity b = state.add(2, UnitType.BLASTER, 6, 5, 0);
        state.damage(a.id(), 50);
        state.damage(b.id(), 50);
        engine.step(state, Map.of(1, List.of(BotAction.attack(a.id(), b.id())),
                2, List.of(BotAction.attack(b.id(), a.id()))));
        assertNull(state.entity(a.id()));
        assertNull(state.entity(b.id()));
    }

    @Test void duplicateActionBecomesWaitAndIsRecorded() {
        MatchState state = engine.init(config, teams, 1L);
        Entity worker = state.entities(1, UnitType.WORKER).get(0);
        TickResult tick = engine.step(state, Map.of(1, List.of(
                BotAction.move(worker.id(), Direction.SOUTH), BotAction.move(worker.id(), Direction.EAST))));
        assertEquals(2, worker.x());
        assertEquals(1, worker.y());
        assertTrue(tick.events().contains("INVALID " + worker.id()));
    }

    @Test void absentActionIsRecordedAsWait() {
        MatchState state = engine.init(config, teams, 1L);
        int workerId = state.entities(1, UnitType.WORKER).get(0).id();
        TickResult tick = engine.step(state, Map.of());
        assertTrue(tick.events().contains("WAIT " + workerId));
    }

    @Test void tickLimitRanksByCoreHpBeforeOtherScores() {
        MatchState state = engine.init(new GameConfig(16, 1, true, 10, 5), teams, 1L);
        Entity core = state.entities(2, UnitType.CORE).get(0);
        state.damage(core.id(), 1);
        engine.step(state, Map.of());
        assertEquals(1, engine.results(state).winnerSlot());
    }
}
