package com.vbforge.contacts;

/**
 * One address-book entry. Immutable.
 *
 * The original MyContact had commented-out getters, which meant the UI
 * could never read a contact's values back — so "Edit" silently replaced
 * the selected row instead of editing it. A record's accessors are
 * generated and always present, so that bug is structurally impossible.
 */
public record Contact(long id, String name, String phone, String email) {

    public Contact withId(long newId) {
        return new Contact(newId, name, phone, email);
    }

    public Contact withFields(String newName, String newPhone, String newEmail) {
        return new Contact(id, newName, newPhone, newEmail);
    }

    /** True if the query appears (case-insensitively) in any field. */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase();
        return name.toLowerCase().contains(q)
            || phone.toLowerCase().contains(q)
            || email.toLowerCase().contains(q);
    }
}