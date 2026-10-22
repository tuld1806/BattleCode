package edu.uet.battlecode.worker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public final class ArtifactExtractor {
    private static final int MAX_ARCHIVE_BYTES = 500 * 1024;
    private static final int MAX_UNPACKED_BYTES = 1024 * 1024;
    private ArtifactExtractor() {}

    public static void extract(byte[] archive, String expectedSha256, Path destination) throws IOException {
        if (archive.length > MAX_ARCHIVE_BYTES || expectedSha256 == null ||
                !expectedSha256.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid artifact");
        try {
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(archive);
            if (!MessageDigest.isEqual(actual, HexFormat.of().parseHex(expectedSha256))) {
                throw new IllegalArgumentException("Artifact checksum mismatch");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
        Path root = destination.toAbsolutePath().normalize();
        Files.createDirectories(root);
        int entries = 0, expanded = 0;
        try (TarArchiveInputStream tar = new TarArchiveInputStream(
                new GZIPInputStream(new ByteArrayInputStream(archive)))) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextTarEntry()) != null) {
                if (++entries > 32 || entry.isSymbolicLink() || entry.isLink() || entry.isCharacterDevice() ||
                        entry.isBlockDevice() || entry.isFIFO()) throw new IllegalArgumentException("Unsafe artifact entry");
                Path path = root.resolve(entry.getName()).normalize();
                if (!path.startsWith(root) || path.equals(root)) throw new IllegalArgumentException("Archive path traversal");
                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                    continue;
                }
                if (!entry.isFile() || entry.getSize() < 0 || entry.getSize() > MAX_UNPACKED_BYTES ||
                        expanded + entry.getSize() > MAX_UNPACKED_BYTES) {
                    throw new IllegalArgumentException("Artifact is too large or unsupported");
                }
                expanded += (int) entry.getSize();
                Files.createDirectories(path.getParent());
                try (var output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    tar.transferTo(output);
                }
                try {
                    Set<PosixFilePermission> permissions = EnumSet.of(PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE);
                    if ((entry.getMode() & 0100) != 0) permissions.add(PosixFilePermission.OWNER_EXECUTE);
                    Files.setPosixFilePermissions(path, permissions);
                } catch (UnsupportedOperationException ignored) {
                    // Windows unit tests do not have POSIX permissions.
                }
            }
        }
    }
}
