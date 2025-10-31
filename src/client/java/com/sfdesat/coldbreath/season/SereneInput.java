package com.sfdesat.coldbreath.season;

import com.sfdesat.coldbreath.season.SeasonDetector.SeasonMod;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Adapter that talks to the Serene Seasons API (if present) and translates its
 * data to the abstraction used by {@link SeasonManager}.
 */
public final class SereneInput {

    private static final Logger LOGGER = LogManager.getLogger("ColdBreath/SereneInput");

    private SereneInput() {}

    public static SeasonSnapshot sample(ClientWorld world, SeasonMod mod) {
        if (world == null) {
            return SeasonSnapshot.empty(mod);
        }

        if (mod == SeasonMod.SERENE_SEASONS) {
            SeasonSnapshot real = SereneBridge.INSTANCE.sample(world);
            if (real != null) {
                return real;
            }
        }

        return placeholder(world, mod);
    }

    private static SeasonSnapshot placeholder(ClientWorld world, SeasonMod mod) {
        if (mod == SeasonMod.VANILLA) {
            return SeasonSnapshot.empty(mod);
        }

        long days = world.getTimeOfDay() / 24000L;
        int index = (int) Math.floorMod(days, SeasonPhase.orderedValues().length);
        SeasonPhase phase = SeasonPhase.fromOrdinal(index);
        return new SeasonSnapshot(mod, phase, 0.0D, 0, 0);
    }

    public record SeasonSnapshot(
            SeasonMod mod,
            SeasonPhase phase,
            double temperatureBias,
            int dayInCycle,
            int cycleTicks
    ) {
        public static SeasonSnapshot empty(SeasonMod mod) {
            return new SeasonSnapshot(mod, SeasonPhase.UNKNOWN, 0.0D, 0, 0);
        }
    }

    private static final class SereneBridge {
        private static final SereneBridge INSTANCE = new SereneBridge();

        private final boolean available;
        private final MethodHandle getSeasonState;
        private final MethodHandle getSubSeason;
        private final MethodHandle getDay;
        private final MethodHandle getCycleTicks;
        private boolean loggedUnavailable;
        private boolean loggedFailure;

        private SereneBridge() {
            MethodHandle seasonState = null;
            MethodHandle subSeason = null;
            MethodHandle day = null;
            MethodHandle cycle = null;
            boolean ok;
            try {
                Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
                Class<?> state = Class.forName("sereneseasons.api.season.ISeasonState");
                Class<?> subSeasonType = Class.forName("sereneseasons.api.season.Season$SubSeason");

                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                seasonState = lookup.findStatic(helper, "getSeasonState",
                        MethodType.methodType(state, World.class));
                subSeason = lookup.findVirtual(state, "getSubSeason",
                        MethodType.methodType(subSeasonType));
                day = lookup.findVirtual(state, "getDay", MethodType.methodType(int.class));
                cycle = lookup.findVirtual(state, "getSeasonCycleTicks", MethodType.methodType(int.class));
                ok = true;
            } catch (Throwable error) {
                LOGGER.debug("Serene Seasons API unavailable: {}", error.toString());
                ok = false;
            }
            this.available = ok;
            this.getSeasonState = seasonState;
            this.getSubSeason = subSeason;
            this.getDay = day;
            this.getCycleTicks = cycle;
            this.loggedUnavailable = !ok;
        }

        private SeasonSnapshot sample(ClientWorld world) {
            if (!available) {
                if (!loggedUnavailable) {
                    LOGGER.warn("Serene Seasons API unavailable; using placeholder seasons");
                    loggedUnavailable = true;
                }
                return null;
            }
            try {
                Object state = getSeasonState.invoke(world);
                if (state == null) return null;

                Object subSeasonObj = getSubSeason.invoke(state);
                String subSeasonName = subSeasonObj instanceof Enum<?> enumValue
                        ? enumValue.name()
                        : String.valueOf(subSeasonObj);
                SeasonPhase phase = SeasonPhase.fromName(subSeasonName);

                int day = (int) getDay.invoke(state);
                int ticks = (int) getCycleTicks.invoke(state);

                double bias = switch (phase) {
                    case EARLY_WINTER, MID_WINTER, LATE_WINTER -> -0.15D;
                    case EARLY_SUMMER, MID_SUMMER, LATE_SUMMER -> 0.12D;
                    default -> 0.0D;
                };

                return new SeasonSnapshot(SeasonMod.SERENE_SEASONS, phase, bias, day, ticks);
            } catch (Throwable error) {
                if (!loggedFailure) {
                    LOGGER.warn("Failed to query Serene Seasons state: {}", error.toString());
                    loggedFailure = true;
                }
                return null;
            }
        }
    }
}


