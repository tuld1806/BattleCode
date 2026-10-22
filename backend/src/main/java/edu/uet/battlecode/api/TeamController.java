package edu.uet.battlecode.api;

import edu.uet.battlecode.db.TournamentService;
import edu.uet.battlecode.db.CiService;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
public class TeamController {
    private final TournamentService tournaments;
    private final AuthContext auth;
    private final CiService ci;

    public TeamController(TournamentService tournaments, AuthContext auth, CiService ci) {
        this.tournaments = tournaments; this.auth = auth; this.ci = ci;
    }

    public record NewTeam(String name) {}
    public record Submission(String repoUrl, String commitSha, String language) {}

    @PostMapping("/teams")
    public Map<String, UUID> createTeam(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tournamentId,
                                        @RequestBody NewTeam input) {
        UUID user = auth.userId(jwt);
        auth.ensureProfile(user);
        return Map.of("id", tournaments.registerTeam(tournamentId, user, input.name()));
    }

    @PostMapping("/teams/{teamId}/join")
    public Map<String, String> join(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tournamentId,
                                    @PathVariable UUID teamId) {
        UUID user = auth.userId(jwt);
        auth.ensureProfile(user);
        tournaments.joinTeam(tournamentId, teamId, user);
        return Map.of("status", "JOINED");
    }

    @PostMapping("/teams/{teamId}/submissions")
    public Map<String, UUID> submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tournamentId,
                                    @PathVariable UUID teamId, @RequestBody Submission input) {
        UUID user = auth.userId(jwt);
        auth.ensureProfile(user);
        UUID id = tournaments.submit(tournamentId, teamId, user,
                input.repoUrl(), input.commitSha(), input.language());
        ci.schedule(id);
        return Map.of("id", id);
    }
}
