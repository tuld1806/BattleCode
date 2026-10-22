package edu.uet.battlecode.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/api/health") public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "system", "BattleCode-UET Platform API",
            "status", "ONLINE",
            "version", "1.0.0",
            "health", "/api/health",
            "tournaments", "/api/public/tournaments"
        );
    }
}
