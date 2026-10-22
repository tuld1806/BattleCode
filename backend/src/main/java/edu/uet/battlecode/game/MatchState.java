package edu.uet.battlecode.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class MatchState {
    private final GameConfig config;
    private final long seed;
    private final boolean[][] obstacles, nodes;
    private final Map<Integer, Entity> entities = new TreeMap<>();
    private final Map<Integer, Integer> resources = new TreeMap<>();
    private int tick, nextId = 1;

    MatchState(GameConfig config, long seed, boolean[][] obstacles, boolean[][] nodes) {
        this.config = config; this.seed = seed; this.obstacles = obstacles; this.nodes = nodes;
        resources.put(1, 100); resources.put(2, 100);
    }
    public GameConfig config() { return config; }
    public long seed() { return seed; }
    public int tick() { return tick; }
    public int resources(int team) { return resources.getOrDefault(team, 0); }
    public boolean isObstacle(int x, int y) { return obstacles[y][x]; }
    public boolean isResourceNode(int x, int y) { return nodes[y][x]; }
    public List<Entity> entities() { return List.copyOf(entities.values()); }
    public List<Entity> entities(UnitType type) { return entities.values().stream().filter(e -> e.type() == type).toList(); }
    public List<Entity> entities(int team, UnitType type) {
        return entities.values().stream().filter(e -> e.team() == team && e.type() == type).toList();
    }
    public Entity entity(int id) { return entities.get(id); }
    public void damage(int id, int amount) {
        Entity entity = entities.get(id);
        if (entity != null) entity.damage(amount);
    }
    Entity add(int team, UnitType type, int x, int y, int activeFromTick) {
        Entity e = new Entity(nextId++, team, type, x, y, activeFromTick);
        entities.put(e.id(), e);
        return e;
    }
    void spend(int team, int amount) { resources.put(team, resources(team) - amount); }
    void earn(int team, int amount) { resources.put(team, resources(team) + amount); }
    void advance() { tick++; }
    boolean inBounds(int x, int y) { return x >= 0 && y >= 0 && x < config.size() && y < config.size(); }
    boolean occupied(int x, int y) {
        return entities.values().stream().anyMatch(e -> e.hp() > 0 && e.x() == x && e.y() == y);
    }
    void removeDead() { entities.values().removeIf(e -> e.hp() <= 0 && e.type() != UnitType.CORE); }
    public List<Entity> sortedById() { return new ArrayList<>(entities.values()); }
}
