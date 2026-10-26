package com.vbforge.animalpedia.engine;

/** A lookup failed. The message is written to be shown directly to a child. */
public class LookupException extends Exception {

    public enum Kind { OFFLINE, NOT_FOUND, UNUSABLE, SERVER }

    private final Kind kind;

    public LookupException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public LookupException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() { return kind; }
}
