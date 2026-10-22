package edu.uet.battlecode.game;

public record UnitView(int id, int team, UnitType type, int x, int y, int hp) {
    public static UnitView of(Entity entity) {
        return new UnitView(entity.id(), entity.team(), entity.type(), entity.x(), entity.y(), entity.hp());
    }
}
