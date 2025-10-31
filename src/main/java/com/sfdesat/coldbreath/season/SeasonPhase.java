package com.sfdesat.coldbreath.season;

import java.util.Locale;

/**
 * Represents a unified view of the twelve sub-seasons used by Serene Seasons
 * (or any other seasonal provider), plus a fallback UNKNOWN value when the
 * season cannot be determined.
 */
public enum SeasonPhase {
    EARLY_SPRING,
    MID_SPRING,
    LATE_SPRING,
    EARLY_SUMMER,
    MID_SUMMER,
    LATE_SUMMER,
    EARLY_AUTUMN,
    MID_AUTUMN,
    LATE_AUTUMN,
    EARLY_WINTER,
    MID_WINTER,
    LATE_WINTER,
    UNKNOWN;

    private static final SeasonPhase[] ORDERED = {
            EARLY_SPRING,
            MID_SPRING,
            LATE_SPRING,
            EARLY_SUMMER,
            MID_SUMMER,
            LATE_SUMMER,
            EARLY_AUTUMN,
            MID_AUTUMN,
            LATE_AUTUMN,
            EARLY_WINTER,
            MID_WINTER,
            LATE_WINTER
    };

    public static SeasonPhase fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= ORDERED.length) return UNKNOWN;
        return ORDERED[ordinal];
    }

    public static SeasonPhase fromName(String name) {
        if (name == null || name.isEmpty()) return UNKNOWN;
        try {
            return SeasonPhase.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }

    public static SeasonPhase[] orderedValues() {
        return ORDERED.clone();
    }

    public String displayName() {
        if (this == UNKNOWN) return "unknown";
        String[] parts = name().toLowerCase(Locale.ROOT).split("_");
        if (parts.length != 2) return name().toLowerCase(Locale.ROOT);
        return parts[0] + " " + parts[1];
    }

    public boolean isWinter() {
        return this == EARLY_WINTER || this == MID_WINTER || this == LATE_WINTER;
    }

    public boolean isSummer() {
        return this == EARLY_SUMMER || this == MID_SUMMER || this == LATE_SUMMER;
    }
}


