package edu.uet.battlecode.game;

import java.util.List;

public record Observation(int tick, long seed, int team, int size, int resources,
                          List<VisibleCell> visibleCells, List<UnitView> friendlyUnits,
                          List<UnitView> visibleEnemies) {}
