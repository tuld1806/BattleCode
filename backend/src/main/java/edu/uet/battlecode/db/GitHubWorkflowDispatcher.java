package edu.uet.battlecode.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class GitHubWorkflowDispatcher implements WorkflowDispatcher {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final URI endpoint;
    private final String token;
    private final String ref;

    public GitHubWorkflowDispatcher(@Value("${battlecode.github-dispatch-repo}") String repo,
                                    @Value("${battlecode.github-dispatch-token}") String token,
                                    @Value("${battlecode.github-dispatch-ref:main}") String ref) {
        if (!repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Invalid CI repository");
        this.endpoint = URI.create("https://api.github.com/repos/" + repo +
                "/actions/workflows/battlecode-submit.yml/dispatches");
        this.token = token; this.ref = ref;
    }

    @Override public void dispatch(CiJobRequest job) {
        try {
            byte[] body = json.writeValueAsBytes(Map.of("ref", ref, "inputs", Map.of(
                    "submission_id", job.submissionId().toString(), "repo_url", job.repoUrl(),
                    "commit_sha", job.commitSha(), "language", job.language(),
                    "callback_token", job.token())));
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 204) throw new IllegalStateException("CI dispatch rejected: HTTP " + response.statusCode());
        } catch (IOException e) {
            throw new IllegalStateException("CI dispatch failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CI dispatch interrupted", e);
        }
    }
}
