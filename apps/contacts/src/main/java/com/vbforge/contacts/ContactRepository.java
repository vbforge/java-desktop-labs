package com.vbforge.contacts;

import java.util.List;

/**
 * Storage abstraction.
 *
 * Two methods: read everything, write everything. Deliberately NOT a
 * fine-grained CRUD interface — a SQL UPDATE has no natural CSV
 * equivalent, so a per-record API would leak one backend's shape into
 * the others. Read-all / write-all works identically for every format,
 * and for a list of contacts of this size it is fast enough.
 */
public interface ContactRepository {

    List<Contact> findAll() throws Exception;

    void saveAll(List<Contact> contacts) throws Exception;

    /** Human-readable, shown in the status bar. */
    default String describe() { return getClass().getSimpleName(); }
}