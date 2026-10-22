package edu.uet.battlecode.runner;

import java.nio.file.Path;

public record BotArtifact(String language, Path directory) {
    public BotArtifact {
        if (!language.equals("python") && !language.equals("cpp") && !language.equals("java")) {
            throw new IllegalArgumentException("Unsupported bot language");
        }
        directory = directory.toAbsolutePath().normalize();
    }
}
