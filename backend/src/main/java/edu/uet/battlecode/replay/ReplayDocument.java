package edu.uet.battlecode.replay;

import edu.uet.battlecode.game.GameConfig;
import edu.uet.battlecode.game.TeamInfo;
import java.util.List;

public record ReplayDocument(String formatVersion, String matchId, long seed, GameConfig config,
                             List<TeamInfo> teams, ReplayMap map, ReplayState initialState,
                             List<ReplayFrame> ticks) {
    public record ReplayMap(int size, List<Integer> obstacles, List<Integer> resourceNodes) {}
}
