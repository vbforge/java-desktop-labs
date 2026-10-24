package com.vbforge.slidecraft.engine.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Temp-file + move writer so a failed export never leaves a truncated file. */
final class AtomicWrite {

    @FunctionalInterface
    interface Writer {
        void write(OutputStream out) throws IOException;
    }

    private AtomicWrite() {
    }

    static void write(Path target, Writer writer) throws IOException {
        Path absolute = target.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path tmp = Files.createTempFile(absolute.getParent(), ".slidecraft-", ".tmp");
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
