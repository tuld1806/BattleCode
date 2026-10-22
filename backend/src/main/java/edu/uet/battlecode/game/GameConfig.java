package edu.uet.battlecode.game;

public record GameConfig(int size, int maxTicks, boolean fogOfWar, int passiveIncome, int mineIncome) {
    public GameConfig {
        if (size < 8 || size > 64 || maxTicks < 1 || maxTicks > 2000 || passiveIncome < 0 || mineIncome < 0) {
            throw new IllegalArgumentException("Invalid game configuration");
        }
    }
}
