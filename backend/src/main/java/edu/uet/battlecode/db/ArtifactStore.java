package edu.uet.battlecode.db;

public interface ArtifactStore {
    String presignPut(String key, String sha256);
    String artifactUrl(String key);
}
