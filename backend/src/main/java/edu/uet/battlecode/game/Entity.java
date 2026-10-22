package edu.uet.battlecode.game;

public final class Entity {
    private final int id, team;
    private final UnitType type;
    private int x, y, hp;
    private int activeFromTick;

    Entity(int id, int team, UnitType type, int x, int y, int activeFromTick) {
        this.id = id; this.team = team; this.type = type; this.x = x; this.y = y;
        this.hp = type.hp; this.activeFromTick = activeFromTick;
    }
    public int id() { return id; }
    public int team() { return team; }
    public UnitType type() { return type; }
    public int x() { return x; }
    public int y() { return y; }
    public int hp() { return hp; }
    public int activeFromTick() { return activeFromTick; }
    void moveTo(int x, int y) { this.x = x; this.y = y; }
    void damage(int amount) { hp = Math.max(0, hp - Math.max(0, amount)); }
}
