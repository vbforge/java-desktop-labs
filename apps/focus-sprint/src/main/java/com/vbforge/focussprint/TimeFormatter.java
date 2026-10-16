package com.vbforge.focussprint;

import java.time.Duration;

public final class TimeFormatter {

    private TimeFormatter() {}

    /** "01:23:47" — always zero-padded, always three components. */
    public static String clock(long seconds) {
        long s = Math.max(0, seconds);
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    /** Human duration: "2h 15m" / "45m" / "12s". */
    public static String duration(long seconds) {
        long s = Math.max(0, seconds);
        if (s >= 3600) return (s / 3600) + "h " + String.format("%02d", (s % 3600) / 60) + "m";
        if (s >= 60)   return (s / 60) + "m";
        return s + "s";
    }

    /** "HH:mm" from a java.time.Instant, in the system zone. */
    public static String timeOf(java.time.Instant instant) {
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(instant);
    }

    public static String humanDate(java.time.LocalDate d) {
        return d.format(java.time.format.DateTimeFormatter.ofPattern("EEE · d MMM yyyy"));
    }

    /** Difference in whole seconds between two instants, floor at 0. */
    public static long secondsBetween(java.time.Instant from, java.time.Instant to) {
        long s = Duration.between(from, to).getSeconds();
        return Math.max(0, s);
    }
}