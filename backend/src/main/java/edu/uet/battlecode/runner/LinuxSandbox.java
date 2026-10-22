package edu.uet.battlecode.runner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class LinuxSandbox {
    private static final Path BWRAP = Path.of("/usr/bin/bwrap");
    private static final Path CGROUP_ROOT = Path.of("/sys/fs/cgroup");

    static void preflight(SandboxConfig config) throws BotProtocolException {
        if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
            throw new BotProtocolException("Official bots require a Linux sandbox");
        }
        if (!Files.isExecutable(BWRAP)) throw new BotProtocolException("bwrap is unavailable");
        Path parent = config.cgroupDirectory().getParent();
        if (parent == null || !parent.startsWith(CGROUP_ROOT) || !Files.isDirectory(parent) ||
                !Files.isWritable(parent) || !Files.exists(parent.resolve("cgroup.controllers"))) {
            throw new BotProtocolException("Writable cgroup v2 parent is unavailable");
        }
        for (Path root : config.runtimeRoots()) {
            if (!root.isAbsolute() || !Files.exists(root)) {
                throw new BotProtocolException("Invalid read-only runtime mount: " + root);
            }
        }
        try {
            Files.createDirectory(config.cgroupDirectory());
            Files.writeString(config.cgroupDirectory().resolve("memory.max"), "100663296", StandardCharsets.US_ASCII);
            Files.writeString(config.cgroupDirectory().resolve("pids.max"), "32", StandardCharsets.US_ASCII);
            Files.writeString(config.cgroupDirectory().resolve("cpu.max"), "50000 100000", StandardCharsets.US_ASCII);
            Files.delete(config.cgroupDirectory());
        } catch (IOException e) {
            try { Files.deleteIfExists(config.cgroupDirectory()); } catch (IOException ignored) { }
            throw new BotProtocolException("cgroup limits cannot be installed", e);
        }
        List<String> probe = baseCommand(config);
        probe.addAll(List.of("--", "/usr/bin/true"));
        try {
            Process process = new ProcessBuilder(probe).start();
            if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) {
                process.destroyForcibly();
                throw new BotProtocolException("bwrap namespace probe failed");
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new BotProtocolException("bwrap namespace probe failed", e);
        }
    }

    static List<String> command(BotArtifact artifact, SandboxConfig config) throws BotProtocolException {
        if (!Files.isDirectory(artifact.directory())) throw new BotProtocolException("Bot artifact directory missing");
        String expected = switch (artifact.language()) {
            case "python" -> "bot.py";
            case "cpp" -> "bot";
            case "java" -> "Bot.class";
            default -> throw new IllegalArgumentException("Unsupported language");
        };
        if (!Files.isRegularFile(artifact.directory().resolve(expected))) {
            throw new BotProtocolException("Bot entry artifact missing: " + expected);
        }
        List<String> command = baseCommand(config);
        command.addAll(List.of("--ro-bind", artifact.directory().toString(), "/bot",
                "--tmpfs", "/tmp", "--proc", "/proc", "--dev", "/dev",
                "--chdir", "/bot", "--setenv", "PATH", "/opt/java/openjdk/bin:/usr/bin:/bin",
                "--setenv", "HOME", "/tmp", "--"));
        // The gate keeps the student program paused until the cgroup limits are applied.
        command.addAll(List.of("/bin/sh", "-c", "IFS= read -r gate; exec \"$@\"", "sh"));
        switch (artifact.language()) {
            case "python" -> command.addAll(List.of("python3", "-u", "/bot/bot.py"));
            case "cpp" -> command.add("/bot/bot");
            case "java" -> command.addAll(List.of("/opt/java/openjdk/bin/java", "-Xmx64m", "-Xss256k",
                    "-XX:MaxMetaspaceSize=32m", "-XX:+UseSerialGC", "-cp", "/bot", "Bot"));
            default -> throw new IllegalArgumentException("Unsupported language");
        }
        return command;
    }

    private static List<String> baseCommand(SandboxConfig config) throws BotProtocolException {
        List<String> command = new ArrayList<>(List.of(BWRAP.toString(), "--die-with-parent",
                "--unshare-all", "--unshare-net", "--clearenv"));
        for (Path root : config.runtimeRoots()) {
            if (Files.isSymbolicLink(root)) {
                try {
                    command.addAll(List.of("--symlink", Files.readSymbolicLink(root).toString(), root.toString()));
                } catch (IOException e) {
                    throw new BotProtocolException("Could not inspect runtime symlink", e);
                }
            } else {
                command.addAll(List.of("--ro-bind", root.toString(), root.toString()));
            }
        }
        return command;
    }

    static Process launch(BotArtifact artifact, SandboxConfig config) throws BotProtocolException {
        preflight(config);
        List<String> command = command(artifact, config);
        Path group = config.cgroupDirectory();
        Process process = null;
        try {
            Files.createDirectory(group);
            Files.writeString(group.resolve("memory.max"), "100663296", StandardCharsets.US_ASCII);
            Files.writeString(group.resolve("pids.max"), "32", StandardCharsets.US_ASCII);
            Files.writeString(group.resolve("cpu.max"), "50000 100000", StandardCharsets.US_ASCII);
            process = new ProcessBuilder(command).start();
            attach(group, process.pid());
            for (ProcessHandle child : process.descendants().toList()) attach(group, child.pid());
            process.getOutputStream().write('\n');
            process.getOutputStream().flush();
            return process;
        } catch (IOException e) {
            if (process != null) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
            try { Files.deleteIfExists(group); } catch (IOException ignored) { }
            throw new BotProtocolException("Sandbox limits could not be applied", e);
        }
    }

    private static void attach(Path group, long pid) throws IOException {
        Files.writeString(group.resolve("cgroup.procs"), Long.toString(pid), StandardCharsets.US_ASCII);
    }
}
