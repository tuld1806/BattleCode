package edu.uet.battlecode.runner;

import edu.uet.battlecode.game.GameConfig;
import edu.uet.battlecode.game.TeamInfo;
import java.util.List;

public record MatchSpec(String matchId, GameConfig config, List<TeamInfo> teams, long seed,
                        BotArtifact botOne, BotArtifact botTwo,
                        SandboxConfig sandboxOne, SandboxConfig sandboxTwo) {
    public MatchSpec {
        teams = List.copyOf(teams);
        if (teams.size() != 2 || teams.get(0).slot() != 1 || teams.get(1).slot() != 2) {
            throw new IllegalArgumentException("Expected team slots 1 and 2");
        }
    }
}
