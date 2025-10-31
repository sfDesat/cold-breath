package com.sfdesat.coldbreath.season;

import net.fabricmc.loader.api.FabricLoader;

public final class SeasonDetector {

    public enum SeasonMod {
        VANILLA,
        SERENE_SEASONS,
        FABRIC_SEASONS
    }

    private static SeasonMod detected = SeasonMod.VANILLA;

    private SeasonDetector() {}

    public static void init() {
        boolean sereneLoaded = isAnyLoaded(
                "sereneseasons",
                "serene_seasons",
                "serene-seasons"
        );

        boolean fabricLoaded = isAnyLoaded(
                "seasons",
                "fabricseasons",
                "fabric-seasons"
        );

        if (sereneLoaded) {
            detected = SeasonMod.SERENE_SEASONS;
        } else if (fabricLoaded) {
            detected = SeasonMod.FABRIC_SEASONS;
        } else {
            detected = SeasonMod.VANILLA;
        }
    }

    public static SeasonMod getDetected() {
        return detected;
    }

    public static String getDisplayName() {
        return switch (detected) {
            case SERENE_SEASONS -> "Serene Seasons";
            case FABRIC_SEASONS -> "Fabric Seasons";
            default -> "Vanilla";
        };
    }

    private static boolean isAnyLoaded(String... ids) {
        for (String id : ids) {
            if (FabricLoader.getInstance().isModLoaded(id)) return true;
        }
        return false;
    }
}


