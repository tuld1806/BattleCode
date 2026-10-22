package edu.uet.battlecode.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public final class CiService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final WorkflowDispatcher dispatcher;
    private final ArtifactStore artifacts;

    private record Submission(String repoUrl, String commitSha, String language) {}
    private record Job(String hash, Instant expiresAt, Instant usedAt, String key, String digest) {}

    public CiService(DataSource source, WorkflowDispatcher dispatcher, ArtifactStore artifacts) {
        this.jdbc = new JdbcTemplate(source);
        this.transactions = new TransactionTemplate(new JdbcTransactionManager(source));
        this.dispatcher = dispatcher;
        this.artifacts = artifacts;
    }

    public void schedule(UUID submissionId) {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        transactions.executeWithoutResult(tx -> {
            Submission submission = jdbc.queryForObject("SELECT repo_url,commit_sha,language FROM submissions WHERE id=?",
                    (rs, row) -> new Submission(rs.getString(1), rs.getString(2), rs.getString(3)), submissionId);
            String key = "artifacts/" + submissionId + "/bot.tar.gz";
            jdbc.update("INSERT INTO ci_jobs(submission_id,token_hash,expires_at,artifact_key) VALUES (?,?,?,?)",
                    submissionId, hash(token), Timestamp.from(Instant.now().plus(30, ChronoUnit.MINUTES)), key);
            dispatcher.dispatch(new CiJobRequest(submissionId, submission.repoUrl(),
                    submission.commitSha().trim(), submission.language(), token));
        });
    }

    public String prepareUpload(UUID submissionId, String token, String digest) {
        requireDigest(digest);
        return Objects.requireNonNull(transactions.execute(tx -> {
            Job job = validJob(submissionId, token);
            if (job == null || (job.digest() != null && !job.digest().equals(digest))) {
                throw new IllegalArgumentException("Invalid CI token or digest");
            }
            jdbc.update("UPDATE ci_jobs SET artifact_sha256=? WHERE submission_id=?", digest, submissionId);
            return artifacts.presignPut(job.key(), digest);
        }));
    }

    public boolean complete(UUID submissionId, String token, boolean passed, String digest,
                            String validationLogUrl, String compileLogUrl, String summary) {
        if (summary != null && summary.length() > 255) throw new IllegalArgumentException("Summary too long");
        if (validationLogUrl != null && !validationLogUrl.startsWith("https://")) throw new IllegalArgumentException("Invalid log URL");
        if (compileLogUrl != null && !compileLogUrl.startsWith("https://")) throw new IllegalArgumentException("Invalid log URL");
        return Objects.requireNonNull(transactions.execute(tx -> {
            Job job = validJob(submissionId, token);
            if (job == null) return false;
            if (passed && (digest == null || !digest.equals(job.digest()))) return false;
            jdbc.update("UPDATE ci_jobs SET used_at=CURRENT_TIMESTAMP WHERE submission_id=?", submissionId);
            jdbc.update("UPDATE submissions SET status=?, artifact_url=?, artifact_sha256=?, " +
                    "validation_log_url=?, compile_log_url=?, error_summary=? WHERE id=? AND status='PENDING'",
                    passed ? "PASSED" : "FAILED", passed ? artifacts.artifactUrl(job.key()) : null,
                    passed ? digest : null, validationLogUrl, compileLogUrl, summary, submissionId);
            return true;
        }));
    }

    private Job validJob(UUID id, String token) {
        if (token == null) return null;
        List<Job> jobs = jdbc.query("SELECT token_hash,expires_at,used_at,artifact_key,artifact_sha256 " +
                "FROM ci_jobs WHERE submission_id=? FOR UPDATE", (rs, row) -> new Job(
                rs.getString(1), rs.getTimestamp(2).toInstant(),
                rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toInstant(),
                rs.getString(4), rs.getString(5)), id);
        if (jobs.isEmpty()) return null;
        Job job = jobs.get(0);
        return job.usedAt() == null && job.expiresAt().isAfter(Instant.now()) &&
                MessageDigest.isEqual(job.hash().getBytes(StandardCharsets.US_ASCII),
                        hash(token).getBytes(StandardCharsets.US_ASCII)) ? job : null;
    }

    private static void requireDigest(String digest) {
        if (digest == null || !digest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("SHA-256 digest required");
        }
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
