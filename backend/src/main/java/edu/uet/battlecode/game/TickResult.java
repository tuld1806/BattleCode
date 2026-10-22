package edu.uet.battlecode.game;

import java.util.List;

public record TickResult(int tick, List<String> events, boolean terminal) {}
