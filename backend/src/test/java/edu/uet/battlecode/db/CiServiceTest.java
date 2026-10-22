package edu.uet.battlecode.db;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class CiServiceTest {
    @Test void callbackTokenIsOneUseAndDigestMustMatchPreparedUpload() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:ci" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE submissions(id UUID PRIMARY KEY,repo_url VARCHAR(500),commit_sha CHAR(40)," +
                "language VARCHAR(8),status VARCHAR(16),artifact_url VARCHAR(1000),artifact_sha256 CHAR(64)," +
                "validation_log_url VARCHAR(1000),compile_log_url VARCHAR(1000),error_summary VARCHAR(255))");
        jdbc.execute("CREATE TABLE ci_jobs(submission_id UUID PRIMARY KEY,token_hash CHAR(64),expires_at TIMESTAMP," +
                "used_at TIMESTAMP,artifact_key VARCHAR(500),artifact_sha256 CHAR(64))");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO submissions(id,repo_url,commit_sha,language,status) VALUES (?,?,?,'python','PENDING')",
                id, "https://github.com/u/repo", "a".repeat(40));
        AtomicReference<CiJobRequest> dispatched = new AtomicReference<>();
        ArtifactStore store = new ArtifactStore() {
            public String presignPut(String key, String sha256) { return "https://upload.example/" + key; }
            public String artifactUrl(String key) { return "https://cdn.example/" + key; }
        };
        CiService ci = new CiService(source, dispatched::set, store);
        ci.schedule(id);
        String token = dispatched.get().token();
        String digest = "b".repeat(64);
        assertEquals("https://upload.example/artifacts/" + id + "/bot.tar.gz",
                ci.prepareUpload(id, token, digest));
        assertFalse(ci.complete(id, "wrong-token", true, digest, null, null, null));
        assertFalse(ci.complete(id, token, true, "c".repeat(64), null, null, null));
        assertTrue(ci.complete(id, token, true, digest, null, null, null));
        assertFalse(ci.complete(id, token, true, digest, null, null, null));
        assertEquals("PASSED", jdbc.queryForObject("SELECT status FROM submissions WHERE id=?", String.class, id));
    }
}
