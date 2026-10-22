package edu.uet.battlecode.api;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public final class AuthContext {
    private final JdbcTemplate jdbc;
    public AuthContext(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public UUID userId(Jwt jwt) {
        if (jwt == null) throw new AccessDeniedException("Authentication required");
        try { return UUID.fromString(jwt.getSubject()); }
        catch (IllegalArgumentException e) { throw new AccessDeniedException("Invalid subject"); }
    }

    public void ensureProfile(UUID userId) {
        jdbc.update("INSERT INTO profiles(id,role) VALUES (?,'STUDENT') ON CONFLICT (id) DO NOTHING", userId);
    }

    public void requireAdmin(Jwt jwt) {
        UUID userId = userId(jwt);
        List<String> roles = jdbc.query("SELECT role FROM profiles WHERE id=?",
                (rs, row) -> rs.getString(1), userId);
        if (roles.size() != 1 || !"ADMIN".equals(roles.get(0))) {
            throw new AccessDeniedException("Admin role required");
        }
    }
}
