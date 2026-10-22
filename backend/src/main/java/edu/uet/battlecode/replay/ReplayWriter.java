package edu.uet.battlecode.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import edu.uet.battlecode.game.BotAction;
import edu.uet.battlecode.game.CyberGridEngine;
import edu.uet.battlecode.game.MatchState;
import edu.uet.battlecode.game.Observation;
import edu.uet.battlecode.game.TeamInfo;
import edu.uet.battlecode.game.TickResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public final class ReplayWriter {
    static final ObjectMapper JSON = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final String matchId;
    private final MatchState initial;
    private final List<TeamInfo> teams;
    private final CyberGridEngine engine;
    private final ReplayState initialState;
    private final ReplayDocument.ReplayMap map;
    private final List<ReplayFrame> frames = new ArrayList<>();
    private ReplayState previous;

    public ReplayWriter(String matchId, MatchState state, List<TeamInfo> teams, CyberGridEngine engine) {
        this.matchId = matchId;
        this.initial = state;
        this.teams = List.copyOf(teams);
        this.engine = engine;
        this.initialState = ReplayState.capture(state, engine);
        this.previous = initialState;
        List<Integer> obstacles = new ArrayList<>(), nodes = new ArrayList<>();
        int size = state.config().size();
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            if (state.isObstacle(x, y)) obstacles.add(y * size + x);
            if (state.isResourceNode(x, y)) nodes.add(y * size + x);
        }
        this.map = new ReplayDocument.ReplayMap(size, List.copyOf(obstacles), List.copyOf(nodes));
    }

    public void append(MatchState state, TickResult tick) {
        append(state, tick, Map.of(), Map.of(), Map.of());
    }

    public void append(MatchState state, TickResult tick, Map<Integer, List<BotAction>> actions,
                       Map<Integer, Observation> observations, Map<Integer, String> debugLogs) {
        if (state.tick() != previous.tick() + 1 || tick.tick() != state.tick()) {
            throw new IllegalArgumentException("Replay ticks must be consecutive");
        }
        ReplayState now = ReplayState.capture(state, engine);
        boolean keyframe = state.tick() % 50 == 0;
        frames.add(new ReplayFrame(state.tick(), keyframe ? now : null,
                keyframe ? null : ReplayDelta.between(previous, now), tick.events(),
                actions, observations, debugLogs));
        previous = now;
    }

    public byte[] toGzip() throws IOException {
        ReplayDocument document = new ReplayDocument("1.0", matchId, initial.seed(), initial.config(),
                teams, map, initialState, List.copyOf(frames));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            JSON.writeValue(gzip, document);
        }
        return bytes.toByteArray();
    }
}
