package edu.uet.battlecode.replay;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;

public final class ReplayReader {
    private final ReplayDocument document;

    public ReplayReader(byte[] gzip) throws IOException {
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(gzip))) {
            document = ReplayWriter.JSON.readValue(input, ReplayDocument.class);
        }
        if (!"1.0".equals(document.formatVersion())) throw new IOException("Unsupported replay version");
    }

    public List<ReplayFrame> frames() { return document.ticks(); }
    public ReplayDocument document() { return document; }

    public ReplayState reconstruct(int tick) {
        if (tick < 0 || tick > frames().size()) throw new IllegalArgumentException("Tick out of range");
        if (tick == 0) return document.initialState();
        int keyframeTick = (tick / 50) * 50;
        ReplayState state = keyframeTick == 0 ? document.initialState()
                : frames().get(keyframeTick - 1).keyframe();
        for (int i = keyframeTick + 1; i <= tick; i++) {
            ReplayFrame frame = frames().get(i - 1);
            state = frame.keyframe() != null ? frame.keyframe() : state.apply(frame.delta());
        }
        return state;
    }
}
