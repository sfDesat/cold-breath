package com.sfdesat.coldbreath.season;

import com.sfdesat.coldbreath.season.SereneInput.SeasonSnapshot;
import com.sfdesat.coldbreath.season.SeasonDetector.SeasonMod;
import com.sfdesat.config.ColdBreathConfig;
import net.minecraft.client.world.ClientWorld;

import java.util.EnumMap;
import java.util.Map;

/**
 * Central manager responsible for keeping track of the current season and exposing
 * derived environmental adjustments (temperature bias, morning-breath toggles, etc.).
 */
public final class SeasonManager {

    private static final EnumMap<SeasonPhase, SeasonAdjustment> ADJUSTMENTS = new EnumMap<>(SeasonPhase.class);

    private static boolean seasonsEnabled = true;
    private static boolean sereneSeasonsEnabled = true;

    private static SeasonSnapshot latest = SeasonSnapshot.empty(SeasonDetector.getDetected());

    static {
        ColdBreathConfig defaults = new ColdBreathConfig();
        defaults.normalizeSeasonConfig();
        applyConfig(defaults);
    }

    private SeasonManager() {}

    public static void applyConfig(ColdBreathConfig cfg) {
        cfg.normalizeSeasonConfig();
        seasonsEnabled = cfg.seasonsEnabled;
        sereneSeasonsEnabled = cfg.sereneSeasonsIntegration;

        ADJUSTMENTS.clear();
        SeasonPhase[] phases = SeasonPhase.orderedValues();
        for (int i = 0; i < phases.length; i++) {
            ADJUSTMENTS.put(phases[i], new SeasonAdjustment(
                    cfg.seasonTemperatureOffsets[i],
                    cfg.seasonMorningBreath[i],
                    0,
                    0
            ));
        }

        if (!seasonsEnabled) {
            latest = SeasonSnapshot.empty(SeasonMod.VANILLA);
        }
    }

    /**
     * Refreshes the cached season snapshot. Call this only when a breath attempt is about to be processed.
     */
    public static void refresh(ClientWorld world) {
        if (!seasonsEnabled) {
            latest = SeasonSnapshot.empty(SeasonMod.VANILLA);
            return;
        }

        SeasonMod mod = SeasonDetector.getDetected();
        if (mod == SeasonMod.SERENE_SEASONS && !sereneSeasonsEnabled) {
            mod = SeasonMod.VANILLA;
        }

        latest = SereneInput.sample(world, mod);
    }

    public static double getTemperatureOffset() {
        if (!seasonsEnabled || latest.mod() == SeasonMod.VANILLA) {
            return 0.0D;
        }
        SeasonAdjustment adjustment = ADJUSTMENTS.getOrDefault(latest.phase(), SeasonAdjustment.DEFAULT);
        return adjustment.temperatureOffset() + latest.temperatureBias();
    }

    public static SeasonPhase getCurrentPhase() {
        return latest.phase();
    }

    public static SeasonMod getCurrentMod() {
        return seasonsEnabled ? latest.mod() : SeasonMod.VANILLA;
    }

    public static boolean isMorningBreathEnabled(boolean defaultValue) {
        if (!seasonsEnabled || latest.mod() == SeasonMod.VANILLA) {
            return defaultValue;
        }
        SeasonAdjustment adjustment = ADJUSTMENTS.getOrDefault(latest.phase(), SeasonAdjustment.DEFAULT);
        return adjustment.morningBreathEnabled();
    }

    public static Map<SeasonPhase, SeasonAdjustment> getAdjustmentsView() {
        return Map.copyOf(ADJUSTMENTS);
    }

    public static SeasonSnapshot getSnapshot() {
        return latest;
    }

    public record SeasonAdjustment(double temperatureOffset, boolean morningBreathEnabled,
                                   int morningStartShift, int morningEndShift) {
        private static final SeasonAdjustment DEFAULT = new SeasonAdjustment(0.0D, true, 0, 0);
    }
}


