package com.matrix.launcher.core;

import java.util.Locale;

/** Pure policies shared by the TV reader and its unit tests. */
public final class FeedRules {
    public static final int MAX_ITEMS = 12;
    public static final int MAX_CHANNELS = 64;
    public static final long MAX_AGE_MS = 90L * 24 * 60 * 60 * 1000;

    public static boolean continuing(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return n.contains("continue") || n.contains("resume") || n.contains("watch next");
    }

    /** Android TV provider constants: 2:3, movie-poster and 3:4. Unknown defaults stay landscape. */
    public static boolean portraitAspect(int value) { return value == 4 || value == 5 || value == 6; }

    public static boolean useful(long position, long duration, long engaged, long now) {
        if (duration > 0 && position >= duration * .98) return false;
        return engaged <= 0 || engaged >= now - MAX_AGE_MS;
    }

    public static int compare(long engagedA, long weightA, long engagedB, long weightB) {
        return engagedA != engagedB ? Long.compare(engagedB, engagedA) : Long.compare(weightB, weightA);
    }
}
