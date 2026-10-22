package edu.uet.battlecode.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import edu.uet.battlecode.game.ActionType;
import edu.uet.battlecode.game.BotAction;
import edu.uet.battlecode.game.Direction;
import edu.uet.battlecode.game.Observation;
import edu.uet.battlecode.game.UnitType;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public final class BotSession implements AutoCloseable {
    public static final int MAX_LINE_BYTES = 16_384;
    private static final int MAX_DEBUG_CHARS = 30_000;
    private static final ObjectMapper JSON = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private final Process process;
    private final BufferedOutputStream input;
    private final BlockingQueue<OutputLine> output = new ArrayBlockingQueue<>(4);
    private final StringBuilder debug = new StringBuilder();
    private long timeBankNanos;
    private int debugCursor;
    private volatile boolean closed;

    private record OutputLine(String value, boolean oversized) {}

    private BotSession(Process process, long bankMs) {
        this.process = process;
        this.input = new BufferedOutputStream(process.getOutputStream());
        this.timeBankNanos = TimeUnit.MILLISECONDS.toNanos(bankMs);
        Thread.ofVirtual().name("bot-stdout").start(() -> drainStdout(process.getInputStream()));
        Thread.ofVirtual().name("bot-stderr").start(() -> drainStderr(process.getErrorStream()));
    }

    public static BotSession start(BotArtifact artifact, SandboxConfig config) throws BotProtocolException {
        return launch(LinuxSandbox.launch(artifact, config), 2_000);
    }

    public static void preflight(SandboxConfig config) throws BotProtocolException {
        LinuxSandbox.preflight(config);
    }

    static BotSession startTrustedForTest(List<String> command, long bankMs) throws BotProtocolException {
        return launch(command, bankMs);
    }

    private static BotSession launch(List<String> command, long bankMs) throws BotProtocolException {
        try {
            return launch(new ProcessBuilder(command).start(), bankMs);
        } catch (IOException e) {
            throw new BotProtocolException("Bot startup failed", e);
        }
    }

    private static BotSession launch(Process process, long bankMs) throws BotProtocolException {
        try {
            BotSession session = new BotSession(process, bankMs);
            OutputLine ready = session.output.poll(1_000, TimeUnit.MILLISECONDS);
            if (ready == null || ready.oversized() || !"READY".equals(ready.value())) {
                session.close();
                throw new BotProtocolException("Bot did not emit READY within 1000 ms");
            }
            return session;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BotProtocolException("Bot startup failed", e);
        }
    }

    public synchronized BotTurnResult request(Observation observation) {
        if (closed || !process.isAlive()) return failure("DISQUALIFIED_CRASH", 0);
        if (!output.isEmpty()) return failure("DISQUALIFIED_PROTOCOL", 0);
        long started = System.nanoTime();
        try {
            input.write(JSON.writeValueAsBytes(observation));
            input.write('\n');
            input.flush();
            long allowed = TimeUnit.MILLISECONDS.toNanos(50) + timeBankNanos;
            OutputLine line = output.poll(allowed, TimeUnit.NANOSECONDS);
            long elapsed = System.nanoTime() - started;
            if (line == null) return failure(process.isAlive() ? "DISQUALIFIED_TIMEOUT" : "DISQUALIFIED_CRASH", elapsed);
            if (line.oversized()) return failure("DISQUALIFIED_OUTPUT", elapsed);
            timeBankNanos = Math.max(0, timeBankNanos - Math.max(0, elapsed - TimeUnit.MILLISECONDS.toNanos(50)));
            ParsedActions parsed = parseActions(line.value());
            return new BotTurnResult(parsed.actions(), parsed.error(), false, elapsed,
                    TimeUnit.NANOSECONDS.toMillis(timeBankNanos), debugSinceLastTurn());
        } catch (IOException e) {
            return failure("DISQUALIFIED_CRASH", System.nanoTime() - started);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure("DISQUALIFIED_INTERRUPTED", System.nanoTime() - started);
        }
    }

    private record ParsedActions(List<BotAction> actions, String error) {}

    private ParsedActions parseActions(String line) {
        try {
            JsonNode root = JSON.readTree(line);
            if (!root.isObject() || !root.path("actions").isArray()) return new ParsedActions(List.of(), "MALFORMED_ACTION");
            List<BotAction> actions = new ArrayList<>();
            boolean malformed = false;
            for (JsonNode item : root.path("actions")) {
                try {
                    if (!item.isObject() || !item.path("unit_id").isIntegralNumber() || !item.path("type").isTextual()) {
                        malformed = true; continue;
                    }
                    int id = item.path("unit_id").intValue();
                    ActionType type = ActionType.valueOf(item.path("type").textValue());
                    Direction direction = item.hasNonNull("direction")
                            ? Direction.valueOf(item.path("direction").textValue()) : null;
                    Integer target = item.path("target_id").isIntegralNumber() ? item.path("target_id").intValue() : null;
                    UnitType unitType = item.hasNonNull("unit_type")
                            ? UnitType.valueOf(item.path("unit_type").textValue()) : null;
                    actions.add(new BotAction(id, type, direction, target, unitType));
                } catch (IllegalArgumentException | NullPointerException e) {
                    malformed = true;
                }
            }
            return new ParsedActions(actions, malformed ? "MALFORMED_ACTION" : null);
        } catch (Exception e) {
            return new ParsedActions(List.of(), "MALFORMED_ACTION");
        }
    }

    private BotTurnResult failure(String reason, long elapsed) {
        String logs = debugSinceLastTurn();
        close();
        return new BotTurnResult(List.of(), reason, true, elapsed,
                TimeUnit.NANOSECONDS.toMillis(timeBankNanos), logs);
    }

    private void drainStdout(InputStream stream) {
        try (stream) {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            boolean oversized = false;
            int byteValue;
            while ((byteValue = stream.read()) != -1) {
                if (byteValue == '\n') {
                    output.put(new OutputLine(line.toString(StandardCharsets.UTF_8).replaceFirst("\\r$", ""), oversized));
                    line.reset(); oversized = false;
                } else if (line.size() < MAX_LINE_BYTES) {
                    line.write(byteValue);
                } else {
                    oversized = true;
                }
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    private void drainStderr(InputStream stream) {
        try (stream) {
            byte[] bytes = new byte[1024];
            int count;
            while ((count = stream.read(bytes)) != -1) {
                synchronized (debug) {
                    int remaining = MAX_DEBUG_CHARS - debug.length();
                    if (remaining > 0) {
                        String chunk = new String(bytes, 0, count, StandardCharsets.UTF_8);
                        debug.append(chunk, 0, Math.min(remaining, chunk.length()));
                    }
                }
            }
        } catch (IOException ignored) {
            // Closing the process also closes the pipe.
        }
    }

    private String debugSinceLastTurn() {
        synchronized (debug) {
            String logs = debug.substring(Math.min(debugCursor, debug.length()));
            debugCursor = debug.length();
            return logs.length() <= 200 ? logs : logs.substring(0, 200);
        }
    }

    public boolean isAlive() { return !closed && process.isAlive(); }

    @Override public void close() {
        if (closed) return;
        closed = true;
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try { input.close(); } catch (IOException ignored) { }
    }
}
