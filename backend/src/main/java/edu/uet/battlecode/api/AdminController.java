package edu.uet.battlecode.api;

import edu.uet.battlecode.db.BracketService;
import edu.uet.battlecode.db.MatchQueueService;
import edu.uet.battlecode.db.TournamentService;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tournaments")
public class AdminController {
    private final AuthContext auth;
    private final TournamentService tournaments;
    private final BracketService bracket;
    private final MatchQueueService queue;
    private final JdbcTemplate jdbc;

    public AdminController(AuthContext auth, TournamentService tournaments, BracketService bracket,
                           MatchQueueService queue, JdbcTemplate jdbc) {
        this.auth = auth; this.tournaments = tournaments; this.bracket = bracket;
        this.queue = queue; this.jdbc = jdbc;
    }

    public record NewTournament(String title, Long seed) {}

    @PostMapping
    public Map<String, UUID> create(@AuthenticationPrincipal Jwt jwt, @RequestBody NewTournament input) {
        auth.requireAdmin(jwt);
        if (input.title() == null || input.title().isBlank() || input.title().length() > 160) {
            throw new IllegalArgumentException("Invalid title");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tournaments(id,title,status,seed) VALUES (?,?,'REGISTRATION_OPEN',?)",
                id, input.title().trim(), input.seed() == null ? id.getMostSignificantBits() : input.seed());
        return Map.of("id", id);
    }

    @PostMapping("/{id}/freeze")
    public Map<String, String> freeze(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        auth.requireAdmin(jwt);
        tournaments.freeze(id);
        return Map.of("status", "SUBMISSION_FROZEN");
    }

    @PostMapping("/{id}/generate")
    public Map<String, String> generate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        auth.requireAdmin(jwt);
        bracket.generate(id);
        return Map.of("status", "RUNNING_MATCHES");
    }

    @PostMapping("/{id}/matches/{matchId}/retry")
    public Map<String, Boolean> retry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @PathVariable UUID matchId) {
        auth.requireAdmin(jwt);
        Integer belongs = jdbc.queryForObject("SELECT count(*) FROM matches m JOIN series s ON s.id=m.series_id " +
                "JOIN rounds r ON r.id=s.round_id WHERE m.id=? AND r.tournament_id=?", Integer.class, matchId, id);
        return Map.of("accepted", belongs != null && belongs == 1 && queue.retrySystemError(matchId));
    }

    @PostMapping("/{id}/publish")
    public Map<String, Boolean> publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        auth.requireAdmin(jwt);
        return Map.of("accepted", bracket.publish(id));
    }
}
