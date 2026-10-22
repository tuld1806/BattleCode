package edu.uet.battlecode.runner;

import edu.uet.battlecode.game.MatchResult;

public record MatchOutcome(MatchResult result, byte[] replayGzip, int ticks) {}
