package com.vbforge.contacts.repo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vbforge.contacts.Contact;
import com.vbforge.contacts.ContactRepository;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonContactRepository implements ContactRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<ContactDto>>() {}.getType();

    private final Path file;

    public JsonContactRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Contact> findAll() throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();

        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<ContactDto> dtos = GSON.fromJson(r, LIST_TYPE);
            if (dtos == null) return new ArrayList<>();

            List<Contact> out = new ArrayList<>(dtos.size());
            for (ContactDto d : dtos) {
                out.add(new Contact(d.id, nz(d.name), nz(d.phone), nz(d.email)));
            }
            return out;
        } catch (Exception e) {
            // Corrupt or hand-edited file → start empty rather than crash.
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(List<Contact> contacts) throws IOException {
        List<ContactDto> dtos = new ArrayList<>(contacts.size());
        for (Contact k : contacts) {
            ContactDto d = new ContactDto();
            d.id    = k.id();
            d.name  = k.name();
            d.phone = k.phone();
            d.email = k.email();
            dtos.add(d);
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(dtos, w);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /**
     * Mutable DTO for Gson.
     *
     * Gson can serialise records since 2.10, but a DTO makes the JSON
     * field names explicit and independent of the record's component
     * order. Renaming or reordering Contact's components will never
     * silently break existing files.
     */
    private static final class ContactDto {
        long id;
        String name;
        String phone;
        String email;
    }

    @Override
    public String describe() {
        return "JSON (" + file.getFileName() + ")";
    }
}