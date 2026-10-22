package edu.uet.battlecode.worker;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;

class ArtifactExtractorTest {
    private byte[] archive(String path) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            byte[] content = "print('READY')".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry(path);
            entry.setSize(content.length);
            tar.putArchiveEntry(entry);
            tar.write(content);
            tar.closeArchiveEntry();
        }
        return bytes.toByteArray();
    }

    @Test void verifiesDigestAndRejectsTraversal() throws Exception {
        Path destination = Files.createTempDirectory(Path.of("target"), "extract-");
        byte[] good = archive("bot.py");
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(good));
        ArtifactExtractor.extract(good, digest, destination);
        assertTrue(Files.isRegularFile(destination.resolve("bot.py")));
        assertThrows(IllegalArgumentException.class,
                () -> ArtifactExtractor.extract(good, "0".repeat(64), destination));
        byte[] bad = archive("../escape.py");
        String badDigest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bad));
        assertThrows(IllegalArgumentException.class,
                () -> ArtifactExtractor.extract(bad, badDigest, destination));
    }
}
