package com.vbforge.animalpedia.engine;

import com.vbforge.animalpedia.model.Animal;

import java.util.List;

/** Where online animal articles come from. Kept as an interface so tests can fake it. */
public interface WikiSource {

    /** Article titles that match the query, best match first. */
    List<String> search(String query) throws LookupException;

    /** The short article for a title. */
    Animal summary(String title) throws LookupException;
}
