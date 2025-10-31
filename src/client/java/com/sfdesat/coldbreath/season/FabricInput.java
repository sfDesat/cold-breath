package com.sfdesat.coldbreath.season;

import com.sfdesat.coldbreath.season.SeasonDetector.SeasonMod;
import net.minecraft.client.world.ClientWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Reflection-based adapter for Fabric Seasons (https://github.com/lucaargolo/fabric-seasons)
 * to keep Cold Breath decoupled from the mod while still integrating when it is present.
 */
public final class FabricInput {

    private static final Logger LOGGER = LogManager.getLogger("ColdBreath/FabricInput");

    private FabricInput() {}

    public static SereneInput.SeasonSnapshot sample(ClientWorld world, SeasonMod mod) {
        if (world == null || mod != SeasonMod.FABRIC_SEASONS) {
            return SereneInput.SeasonSnapshot.empty(mod);
        }

        FabricBridge bridge = FabricBridge.INSTANCE;
        SereneInput.SeasonSnapshot snapshot = bridge.sample(world);
        return snapshot != null ? snapshot : SereneInput.SeasonSnapshot.empty(mod);
    }

    private static final class FabricBridge {
        private static final FabricBridge INSTANCE = new FabricBridge();

        private final boolean available;
        private final MethodHandle getCurrentSeason;
        private final MethodHandle getTemperature;
        private boolean loggedUnavailable;
        private boolean loggedFailure;

        private FabricBridge() {
            MethodHandle seasonMethod = null;
            MethodHandle tempMethod = null;
            boolean ok;
            try {
                Class<?> fabricSeasons = Class.forName("io.github.lucaargolo.seasons.FabricSeasons");
                Class<?> seasonEnum = Class.forName("io.github.lucaargolo.seasons.utils.Season");

                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                seasonMethod = lookup.findStatic(fabricSeasons, "getCurrentSeason",
                        MethodType.methodType(seasonEnum, net.minecraft.world.World.class));
                tempMethod = lookup.findVirtual(seasonEnum, "getTemperature",
                        MethodType.methodType(int.class));
                ok = true;
            } catch (Throwable error) {
                LOGGER.debug("Fabric Seasons API unavailable: {}", error.toString());
                ok = false;
            }
            this.available = ok;
            this.getCurrentSeason = seasonMethod;
            this.getTemperature = tempMethod;
            this.loggedUnavailable = !ok;
        }

        private SereneInput.SeasonSnapshot sample(ClientWorld world) {
            if (!available) {
                if (!loggedUnavailable) {
                    LOGGER.warn("Fabric Seasons API unavailable; using placeholder seasons");
                    loggedUnavailable = true;
                }
                return null;
            }
            try {
                Object seasonObj = getCurrentSeason.invoke(world);
                if (seasonObj == null) return null;

                String seasonName = seasonObj instanceof Enum<?> enumValue
                        ? enumValue.name()
                        : String.valueOf(seasonObj);

                SeasonPhase phase = switch (seasonName) {
                    case "SUMMER" -> SeasonPhase.MID_SUMMER;
                    case "FALL" -> SeasonPhase.MID_AUTUMN;
                    case "WINTER" -> SeasonPhase.MID_WINTER;
                    case "SPRING" -> SeasonPhase.MID_SPRING;
                    default -> SeasonPhase.UNKNOWN;
                };

                double temperatureBias = 0.0D;
                if (getTemperature != null) {
                    int temperatureIndex = (int) getTemperature.invoke(seasonObj);
                    temperatureBias = switch (temperatureIndex) {
                        case 0 -> -0.12D; // winter
                        case 1 -> -0.04D; // autumn
                        case 2 -> 0.05D;  // spring
                        case 3 -> 0.12D;  // summer
                        default -> 0.0D;
                    };
                }

                return new SereneInput.SeasonSnapshot(SeasonMod.FABRIC_SEASONS, phase, temperatureBias, 0, 0);
            } catch (Throwable error) {
                if (!loggedFailure) {
                    LOGGER.warn("Failed to query Fabric Seasons state: {}", error.toString());
                    loggedFailure = true;
                }
                return null;
            }
        }
    }
}

