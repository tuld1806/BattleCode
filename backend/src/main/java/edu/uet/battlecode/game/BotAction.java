package edu.uet.battlecode.game;

public record BotAction(int unitId, ActionType type, Direction direction, Integer targetId, UnitType unitType) {
    public static BotAction waitFor(int id) { return new BotAction(id, ActionType.WAIT, null, null, null); }
    public static BotAction move(int id, Direction direction) { return new BotAction(id, ActionType.MOVE, direction, null, null); }
    public static BotAction attack(int id, int target) { return new BotAction(id, ActionType.ATTACK, null, target, null); }
    public static BotAction spawn(int id, Direction direction, UnitType type) { return new BotAction(id, ActionType.SPAWN, direction, null, type); }
    public static BotAction build(int id, Direction direction) { return new BotAction(id, ActionType.BUILD, direction, null, UnitType.TOWER); }
}
