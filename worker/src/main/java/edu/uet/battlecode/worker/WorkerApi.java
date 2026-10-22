package edu.uet.battlecode.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import edu.uet.battlecode.api.WorkerController.MatchWork;
import edu.uet.battlecode.game.MatchResult;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class WorkerApi {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper json = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final URI base;
    private final String key;
    private final String workerId;

    public WorkerApi(URI base, String key, String workerId) {
        if (!"https".equals(base.getScheme()) &&
                !("http".equals(base.getScheme()) && "127.0.0.1".equals(base.getHost()))) {
            throw new IllegalArgumentException("Worker API must use HTTPS");
        }
        this.base = base; this.key = key; this.workerId = workerId;
    }

    public Optional<UUID> claim() throws IOException, InterruptedException {
        HttpResponse<String> response = send("POST", "/api/worker/claim", Map.of("worker_id", workerId));
        if (response.statusCode() == 204) return Optional.empty();
        requireSuccess(response);
        return Optional.of(UUID.fromString(json.readTree(response.body()).path("id").asText()));
    }

    public MatchWork spec(UUID id) throws IOException, InterruptedException {
        String path = "/api/worker/matches/" + id + "/spec?workerId=" +
                URLEncoder.encode(workerId, StandardCharsets.UTF_8);
        HttpResponse<String> response = send("GET", path, null);
        requireSuccess(response);
        return json.readValue(response.body(), MatchWork.class);
    }

    public boolean heartbeat(UUID id) throws IOException, InterruptedException {
        HttpResponse<String> response = send("POST", "/api/worker/matches/" + id + "/heartbeat",
                Map.of("worker_id", workerId));
        requireSuccess(response);
        return json.readTree(response.body()).path("accepted").asBoolean();
    }

    public boolean complete(UUID id, MatchResult result, String replayUrl, int ticks)
            throws IOException, InterruptedException {
        var body = new java.util.HashMap<String, Object>();
        body.put("worker_id", workerId);
        body.put("winner_slot", result.winnerSlot());
        body.put("replay_url", replayUrl);
        body.put("total_ticks", ticks);
        if (!"SCORE".equals(result.reason()) && !"DRAW".equals(result.reason())) {
            body.put("error_summary", result.reason());
        }
        HttpResponse<String> response = send("POST", "/api/worker/matches/" + id + "/complete", body);
        requireSuccess(response);
        return json.readTree(response.body()).path("accepted").asBoolean();
    }

    public boolean fail(UUID id, String summary) throws IOException, InterruptedException {
        HttpResponse<String> response = send("POST", "/api/worker/matches/" + id + "/fail",
                Map.of("worker_id", workerId, "error_summary", summary.substring(0, Math.min(255, summary.length()))));
        requireSuccess(response);
        return json.readTree(response.body()).path("accepted").asBoolean();
    }

    private HttpResponse<String> send(String method, String path, Object body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(20))
                .header("X-Worker-Key", key).header("Accept", "application/json");
        if ("GET".equals(method)) builder.GET();
        else builder.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void requireSuccess(HttpResponse<String> response) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Worker API returned HTTP " + response.statusCode());
        }
    }
}
