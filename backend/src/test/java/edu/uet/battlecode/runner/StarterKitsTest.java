package edu.uet.battlecode.runner;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StarterKitsTest {
    private final Path kits = Path.of("..", "starter-kits").toAbsolutePath().normalize();
    private final String observation = "{\"tick\":1,\"seed\":41,\"team\":1,\"size\":16," +
            "\"resources\":100,\"visible_cells\":[],\"friendly_units\":[],\"visible_enemies\":[]}";

    @Test void allThreeLanguagesProvideExpectedEntryFiles() {
        assertTrue(Files.isRegularFile(kits.resolve("python/bot.py")));
        assertTrue(Files.isRegularFile(kits.resolve("cpp/src/main.cpp")));
        assertTrue(Files.isRegularFile(kits.resolve("java/src/main/java/Bot.java")));
    }

    @Test void javaStarterHandlesTenPersistentTurns() throws Exception {
        Path output = Files.createTempDirectory(Path.of("target"), "java-kit-");
        Process compile = new ProcessBuilder("javac", "--release", "21", "-d", output.toString(),
                kits.resolve("java/src/main/java/Bot.java").toString()).inheritIO().start();
        assertEquals(0, compile.waitFor());
        smoke(List.of("java", "-cp", output.toString(), "Bot"));
    }

    @Test void cppStarterHandlesTenPersistentTurns() throws Exception {
        Path output = Files.createTempDirectory(Path.of("target"), "cpp-kit-");
        Path binary = output.resolve("bot.exe");
        Process compile = new ProcessBuilder("g++", "-O3", "-std=c++20",
                kits.resolve("cpp/src/main.cpp").toString(), "-o", binary.toString()).inheritIO().start();
        assertEquals(0, compile.waitFor());
        smoke(List.of(binary.toString()));
    }

    private void smoke(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).start();
        try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            assertEquals("READY", output.readLine());
            for (int i = 0; i < 10; i++) {
                input.write(observation); input.newLine(); input.flush();
                String line = output.readLine();
                assertNotNull(line);
                assertTrue(line.startsWith("{\"actions\":"));
            }
        } finally { process.destroyForcibly(); }
    }
}
