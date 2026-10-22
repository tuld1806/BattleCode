package edu.uet.battlecode.replay;

import edu.uet.battlecode.game.BotAction;
import edu.uet.battlecode.game.Observation;
import java.util.List;
import java.util.Map;

public record ReplayFrame(int tick, ReplayState keyframe, ReplayDelta delta, List<String> events,
                          Map<Integer, List<BotAction>> actions, Map<Integer, Observation> observations,
                          Map<Integer, String> debugLogs) {}
