package com.vbforge.slidecraft.engine;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes files via a temp file + move so a crash never leaves a half-written presentation. */
final class AtomicFiles {

    @FunctionalInterface
    interface Writer {
        void write(OutputStream out) throws IOException;
    }

    private AtomicFiles() {
    }

    static void write(Path target, byte[] data) throws IOException {
        write(target, out -> out.write(data));
    }

    static void write(Path target, Writer writer) throws IOException {
        Path absolute = target.toAbsolutePath();
        Path dir = absolute.getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, ".slidecraft-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                writer.write(out);
            }
            try {
                Files.move(tmp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
