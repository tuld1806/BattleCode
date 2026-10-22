package edu.uet.battlecode.replay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record ReplayDelta(int tick, Map<Integer, ReplayState.Unit> upserts, List<Integer> removed,
                          Map<Integer, Integer> resources, Map<Integer, List<Integer>> visibility) {
    public static ReplayDelta between(ReplayState before, ReplayState after) {
        Map<Integer, ReplayState.Unit> upserts = new TreeMap<>();
        for (var entry : after.units().entrySet()) {
            if (!entry.getValue().equals(before.units().get(entry.getKey()))) {
                upserts.put(entry.getKey(), entry.getValue());
            }
        }
        List<Integer> removed = new ArrayList<>();
        for (int id : before.units().keySet()) if (!after.units().containsKey(id)) removed.add(id);
        removed.sort(Integer::compare);
        return new ReplayDelta(after.tick(), upserts, removed, after.resources(), after.visibility());
    }
}
