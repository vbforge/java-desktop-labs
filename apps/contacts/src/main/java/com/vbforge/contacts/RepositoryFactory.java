package com.vbforge.contacts;

import com.vbforge.contacts.repo.CsvContactRepository;
import com.vbforge.contacts.repo.JsonContactRepository;
import com.vbforge.contacts.repo.SqliteContactRepository;

import java.nio.file.Path;
import java.util.Optional;

public final class RepositoryFactory {

    private RepositoryFactory() {}

    /** Repository for a backend, writing to baseDir/<default file name>. */
    public static ContactRepository forBackend(BackendType type, Path baseDir) {
        return create(type, baseDir.resolve(type.defaultFileName()));
    }

    /** Repository pointed at an arbitrary file, backend chosen by extension. */
    public static Optional<ContactRepository> forFile(Path file) {
        BackendType t = BackendType.fromFileName(file.getFileName().toString());
        return t == null ? Optional.empty() : Optional.of(create(t, file));
    }

    private static ContactRepository create(BackendType type, Path file) {
        return switch (type) {
            case SQLITE -> new SqliteContactRepository(file);
            case CSV    -> new CsvContactRepository(file);
            case JSON   -> new JsonContactRepository(file);
        };
    }
}