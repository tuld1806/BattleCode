package edu.uet.battlecode.runner;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

public record SandboxConfig(Path cgroupDirectory, List<Path> runtimeRoots) {
    public SandboxConfig {
        cgroupDirectory = cgroupDirectory.toAbsolutePath().normalize();
        runtimeRoots = List.copyOf(runtimeRoots);
    }

    public static SandboxConfig linux(Path cgroupDirectory) {
        return new SandboxConfig(cgroupDirectory, Stream.of(
                "/usr", "/bin", "/lib", "/lib64", "/etc/ssl",
                "/etc/alternatives", "/opt/java/openjdk")
                .map(Path::of).filter(Files::exists).toList());
    }
}
