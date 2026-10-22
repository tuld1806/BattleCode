package edu.uet.battlecode.api;

import edu.uet.battlecode.db.BracketService;
import edu.uet.battlecode.db.ClaimedMatch;
import edu.uet.battlecode.db.MatchQueueService;
import edu.uet.battlecode.game.GameConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {
    private final MatchQueueService queue;
    private final BracketService bracket;
    private final JdbcTemplate jdbc;
    private final byte[] key;

    public WorkerController(MatchQueueService queue, BracketService bracket, JdbcTemplate jdbc,
                            @Value("${battlecode.worker-key}") String key) {
        if (key.length() < 32) throw new IllegalStateException("WORKER_API_KEY must have at least 32 characters");
        this.queue = queue; this.bracket = bracket; this.jdbc = jdbc;
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    public record WorkerId(String workerId) {}
    public record Completion(String workerId, Integer winnerSlot, String replayUrl,
                             Integer totalTicks, String errorSummary) {}
    public record Failure(String workerId, String errorSummary) {}
    public record MatchWork(UUID id, long seed, GameConfig config, String teamOneName, String teamTwoName,
                            String languageOne, String languageTwo, String artifactUrlOne,
                            String artifactUrlTwo, String digestOne, String digestTwo) {}

    @PostMapping("/claim")
    public ResponseEntity<ClaimedMatch> claim(@RequestHeader(value = "X-Worker-Key", required = false) String supplied,
                                              @RequestBody WorkerId request) {
        verify(supplied);
        Optional<ClaimedMatch> match = queue.claim(request.workerId());
        return match.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/matches/{id}/heartbeat")
    public Map<String, Boolean> heartbeat(@RequestHeader(value = "X-Worker-Key", required = false) String supplied,
                                          @PathVariable UUID id, @RequestBody WorkerId request) {
        verify(supplied);
        return Map.of("accepted", queue.heartbeat(id, request.workerId()));
    }

    @GetMapping("/matches/{id}/spec")
    public MatchWork spec(@RequestHeader(value = "X-Worker-Key", required = false) String supplied,
                          @PathVariable UUID id, @RequestParam String workerId) {
        verify(supplied);
        return jdbc.query("SELECT m.id,m.random_seed,t.map_size,t.max_ticks,a.name,b.name," +
                "sa.language,sb.language,sa.artifact_url,sb.artifact_url," +
                "sa.artifact_sha256,sb.artifact_sha256 FROM matches m " +
                "JOIN series s ON s.id=m.series_id JOIN rounds r ON r.id=s.round_id " +
                "JOIN tournaments t ON t.id=r.tournament_id " +
                "JOIN teams a ON a.id=m.team_one_id JOIN teams b ON b.id=m.team_two_id " +
                "JOIN submissions sa ON sa.id=m.submission_one_id " +
                "JOIN submissions sb ON sb.id=m.submission_two_id " +
                "WHERE m.id=? AND m.status='RUNNING' AND m.worker_id=? " +
                "AND m.lease_expires_at>CURRENT_TIMESTAMP", (rs, row) -> new MatchWork(
                rs.getObject(1, UUID.class), rs.getLong(2),
                new GameConfig(rs.getInt(3), rs.getInt(4), true, 10, 5),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12)),
                id, workerId).stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Active match not found"));
    }

    @PostMapping("/matches/{id}/complete")
    @Transactional
    public Map<String, Boolean> complete(@RequestHeader(value = "X-Worker-Key", required = false) String supplied,
                                         @PathVariable UUID id, @RequestBody Completion request) {
        verify(supplied);
        boolean accepted = queue.complete(id, request.workerId(), request.winnerSlot(),
                request.replayUrl(), request.errorSummary(), request.totalTicks());
        if (accepted) bracket.reconcile(id);
        return Map.of("accepted", accepted);
    }

    @PostMapping("/matches/{id}/fail")
    public Map<String, Boolean> fail(@RequestHeader(value = "X-Worker-Key", required = false) String supplied,
                                     @PathVariable UUID id, @RequestBody Failure request) {
        verify(supplied);
        return Map.of("accepted", queue.failSystem(id, request.workerId(), request.errorSummary()));
    }

    private void verify(String supplied) {
        if (supplied == null || !MessageDigest.isEqual(key, supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid worker key");
        }
    }
}
