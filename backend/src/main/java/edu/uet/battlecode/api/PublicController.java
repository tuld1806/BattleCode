package edu.uet.battlecode.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private static final String CACHE = "public, s-maxage=5, stale-while-revalidate=10";
    private final JdbcTemplate jdbc;
    public PublicController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/tournaments")
    public ResponseEntity<List<Map<String, Object>>> tournaments() {
        return cached(jdbc.queryForList("SELECT id,title,status,map_size,max_ticks,created_at " +
                "FROM tournaments ORDER BY created_at DESC LIMIT 50"));
    }

    @GetMapping("/tournaments/{id}")
    public ResponseEntity<List<Map<String, Object>>> tournament(@PathVariable UUID id) {
        return cached(jdbc.queryForList("SELECT id,title,status,map_size,max_ticks,registration_deadline," +
                "submission_deadline FROM tournaments WHERE id=?", id));
    }

    @GetMapping("/tournaments/{id}/teams")
    public ResponseEntity<List<Map<String, Object>>> teams(@PathVariable UUID id) {
        return cached(jdbc.queryForList("SELECT id,name,seed FROM teams WHERE tournament_id=? ORDER BY seed NULLS LAST,name", id));
    }

    @GetMapping("/tournaments/{id}/bracket")
    public ResponseEntity<List<Map<String, Object>>> bracket(@PathVariable UUID id) {
        return cached(jdbc.queryForList("SELECT r.round_number,s.id,s.position,s.status," +
                "a.name AS team_one,b.name AS team_two,w.name AS winner FROM series s " +
                "JOIN rounds r ON r.id=s.round_id LEFT JOIN teams a ON a.id=s.team_one_id " +
                "LEFT JOIN teams b ON b.id=s.team_two_id LEFT JOIN teams w ON w.id=s.winner_team_id " +
                "WHERE r.tournament_id=? ORDER BY r.round_number,s.position", id));
    }

    @GetMapping("/tournaments/{id}/standings")
    public ResponseEntity<List<Map<String, Object>>> standings(@PathVariable UUID id) {
        return cached(jdbc.queryForList("SELECT t.id,t.name,e.seed,COUNT(s.id) FILTER " +
                "(WHERE s.winner_team_id=t.id) AS series_wins FROM tournament_entries e " +
                "JOIN teams t ON t.id=e.team_id LEFT JOIN series s ON s.winner_team_id=t.id " +
                "WHERE e.tournament_id=? GROUP BY t.id,t.name,e.seed " +
                "ORDER BY series_wins DESC,e.seed", id));
    }

    @GetMapping("/tournaments/{id}/matches")
    public ResponseEntity<List<Map<String, Object>>> matches(@PathVariable UUID id) {
        return cached(jdbc.queryForList("SELECT m.id,m.series_id,m.game_number,m.status,m.winner_slot," +
                "m.total_ticks,m.error_summary FROM matches m JOIN series s ON s.id=m.series_id " +
                "JOIN rounds r ON r.id=s.round_id WHERE r.tournament_id=? " +
                "ORDER BY r.round_number,s.position,m.game_number", id));
    }

    @GetMapping("/matches/{id}")
    public ResponseEntity<List<Map<String, Object>>> match(@PathVariable UUID id) {
        return cached(jdbc.queryForList("SELECT m.id,m.status,m.game_number,m.winner_slot,m.total_ticks," +
                "CASE WHEN t.status='PUBLISHED' THEN m.replay_data_url ELSE NULL END AS replay_data_url " +
                "FROM matches m JOIN series s ON s.id=m.series_id JOIN rounds r ON r.id=s.round_id " +
                "JOIN tournaments t ON t.id=r.tournament_id WHERE m.id=?", id));
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<List<Map<String, Object>>> submission(@PathVariable UUID id) {
        return cached(jdbc.queryForList("SELECT id,team_id,commit_sha,language,status,error_summary,created_at " +
                "FROM submissions WHERE id=?", id));
    }

    private <T> ResponseEntity<T> cached(T body) {
        return ResponseEntity.ok().header("Cache-Control", CACHE).body(body);
    }
}
