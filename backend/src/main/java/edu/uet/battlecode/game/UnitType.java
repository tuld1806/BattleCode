package edu.uet.battlecode.game;

public enum UnitType {
    CORE(1000, 30, 3, 6, 0, false),
    WORKER(50, 5, 1, 5, 50, true),
    PAWN(120, 25, 1, 3, 100, true),
    BLASTER(80, 35, 3, 4, 150, true),
    TOWER(300, 40, 4, 4, 200, false);

    public final int hp, attack, range, vision, cost;
    public final boolean mobile;
    UnitType(int hp, int attack, int range, int vision, int cost, boolean mobile) {
        this.hp = hp; this.attack = attack; this.range = range; this.vision = vision;
        this.cost = cost; this.mobile = mobile;
    }
}
