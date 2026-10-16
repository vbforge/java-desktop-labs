package com.vbforge.focussprint;

import java.time.Instant;

public record Session(long id, long sectionId, Instant startedAt, Instant endedAt, long seconds) {

    public static Session completed(long sectionId, Instant startedAt, Instant endedAt) {
        long secs = Math.max(1, TimeFormatter.secondsBetween(startedAt, endedAt));
        return new Session(0, sectionId, startedAt, endedAt, secs);
    }
}