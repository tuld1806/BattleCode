package edu.uet.battlecode.worker;

import edu.uet.battlecode.api.WorkerController.MatchWork;
import edu.uet.battlecode.game.TeamInfo;
import edu.uet.battlecode.runner.BotArtifact;
import edu.uet.battlecode.runner.BotSession;
import edu.uet.battlecode.runner.MatchExecutor;
import edu.uet.battlecode.runner.MatchOutcome;
import edu.uet.battlecode.runner.MatchSpec;
import edu.uet.battlecode.runner.SandboxConfig;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WorkerMain {
    public static void main(String[] args) throws Exception {
        if (!"1".equals(System.getenv().getOrDefault("WORKER_CONCURRENCY", "1"))) {
            throw new IllegalStateException("Pilot worker concurrency must be 1");
        }
        Path cgroupParent = Path.of(required("WORKER_CGROUP_PARENT")).toAbsolutePath().normalize();
        Path scratch = Path.of(required("WORKER_SCRATCH_DIR")).toAbsolutePath().normalize();
        Files.createDirectories(scratch);
        // This probe must pass before the worker can claim any official match.
        BotSession.preflight(SandboxConfig.linux(cgroupParent.resolve("preflight-" + UUID.randomUUID())));
        WorkerApi api = new WorkerApi(URI.create(required("BATTLECODE_API_BASE")),
                required("WORKER_API_KEY"), required("WORKER_ID"));
        try (WorkerStorage storage = new WorkerStorage(required("R2_ENDPOINT"),
                required("R2_ACCESS_KEY_ID"), required("R2_SECRET_ACCESS_KEY"),
                required("R2_BUCKET"), required("R2_PUBLIC_BASE_URL"))) {
            while (!Thread.currentThread().isInterrupted()) {
                Optional<UUID> id = api.claim();
                if (id.isEmpty()) {
                    Thread.sleep(2_000);
                    continue;
                }
                UUID matchId = id.get();
                Path work = Files.createTempDirectory(scratch, "match-" + matchId + "-");
                try (ScheduledExecutorService heartbeats = Executors.newSingleThreadScheduledExecutor()) {
                    AtomicBoolean leaseLost = new AtomicBoolean();
                    heartbeats.scheduleAtFixedRate(() -> {
                        try { if (!api.heartbeat(matchId)) leaseLost.set(true); }
                        catch (Exception e) { leaseLost.set(true); }
                    }, 30, 30, TimeUnit.SECONDS);
                    try {
                        MatchWork spec = api.spec(matchId);
                        Path one = Files.createDirectory(work.resolve("one"));
                        Path two = Files.createDirectory(work.resolve("two"));
                        ArtifactExtractor.extract(storage.downloadArtifact(spec.artifactUrlOne()), spec.digestOne(), one);
                        ArtifactExtractor.extract(storage.downloadArtifact(spec.artifactUrlTwo()), spec.digestTwo(), two);
                        MatchSpec game = new MatchSpec(matchId.toString(), spec.config(),
                                List.of(new TeamInfo(1, spec.teamOneName()), new TeamInfo(2, spec.teamTwoName())),
                                spec.seed(), new BotArtifact(spec.languageOne(), one),
                                new BotArtifact(spec.languageTwo(), two),
                                SandboxConfig.linux(cgroupParent.resolve("bot-" + matchId + "-1")),
                                SandboxConfig.linux(cgroupParent.resolve("bot-" + matchId + "-2")));
                        MatchOutcome outcome = new MatchExecutor().execute(game);
                        if (leaseLost.get()) throw new IllegalStateException("Match lease was lost");
                        String replayUrl = storage.uploadReplay(matchId, outcome.replayGzip());
                        if (!api.complete(matchId, outcome.result(), replayUrl, outcome.ticks())) {
                            throw new IllegalStateException("Match completion rejected as stale");
                        }
                    } catch (Exception e) {
                        String summary = e.getClass().getSimpleName() + ": " + e.getMessage();
                        try { api.fail(matchId, summary == null ? "Worker error" : summary); }
                        catch (Exception failure) { System.err.println("Could not report match failure: " + failure.getClass().getSimpleName()); }
                    } finally {
                        heartbeats.shutdownNow();
                        cleanup(work, scratch);
                    }
                }
            }
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + name);
        return value;
    }

    private static void cleanup(Path work, Path scratch) throws IOException {
        Path target = work.toAbsolutePath().normalize();
        if (!target.startsWith(scratch) || target.equals(scratch)) throw new IOException("Unsafe cleanup path");
        try (var paths = Files.walk(target)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
