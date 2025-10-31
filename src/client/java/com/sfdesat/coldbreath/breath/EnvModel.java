package com.sfdesat.coldbreath.breath;

import com.sfdesat.coldbreath.season.SeasonManager;
import com.sfdesat.config.ColdBreathConfig;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public final class EnvModel {

	private EnvModel() {}

	public static float computeEffectiveTemperature(ClientWorld world, BlockPos pos, ColdBreathConfig cfg) {
		Biome biome = world.getBiome(pos).value();
		float baseTemperature = biome.getTemperature();
		float temperature = baseTemperature;
		if (cfg.altitudeAdjustmentEnabled) {
			int seaLevel = world.getSeaLevel();
			int altitude = pos.getY();
			temperature = baseTemperature - (altitude - seaLevel) * (float)cfg.altitudeTemperatureRate;
		}
		temperature += (float) SeasonManager.getTemperatureOffset();
		return temperature;
	}

	public static boolean isWithinDayWindow(long dayTime, long start, long end) {
		if (start == end) return false;
		return (start <= end) ? (dayTime >= start && dayTime <= end)
			: (dayTime >= start || dayTime <= end);
	}

	public static DimensionKind getDimensionKind(ClientWorld world) {
		if (world.getRegistryKey() == World.NETHER) return DimensionKind.NETHER;
		if (world.getRegistryKey() == World.END) return DimensionKind.END;
		if (world.getRegistryKey() == World.OVERWORLD) return DimensionKind.OVERWORLD;
		Identifier id = world.getRegistryKey().getValue();
		String path = id.getPath();
		if (path.contains("nether")) return DimensionKind.NETHER;
		if (path.contains("end") || path.contains("the_end")) return DimensionKind.END;
		return DimensionKind.OTHER;
	}

    public static BreathEligibility checkEligibility(ClientWorld world, PlayerEntity player, ColdBreathConfig cfg) {
        if (!cfg.enabled) return BreathEligibility.deny("disabled");
        if (player.isSpectator()) return BreathEligibility.deny("spectator");
        if (player.isSleeping()) return BreathEligibility.deny("sleeping");
        if (player.isDead()) return BreathEligibility.deny("dead");
        if (!cfg.visibleInCreative && player.getAbilities().creativeMode) return BreathEligibility.deny("creative");

        if (player.isSubmergedInWater()) {
            return cfg.underwaterEnabled
                    ? BreathEligibility.allow()
                    : BreathEligibility.deny("underwater");
        }

        DimensionKind dim = getDimensionKind(world);
        if (dim == DimensionKind.NETHER && !cfg.visibleInNether) return BreathEligibility.deny("nether hidden");
        if (dim == DimensionKind.END && !cfg.visibleInEnd) return BreathEligibility.deny("end hidden");

        BlockPos pos = player.getBlockPos();
        float temp = computeEffectiveTemperature(world, pos, cfg);
        boolean isColdHere = temp <= cfg.alwaysBreathTemperature;
        if (cfg.alwaysShowBreath || isColdHere) return BreathEligibility.allow();

        if (!cfg.breathCondensationEnabled) return BreathEligibility.deny("condensation off");

        long dayTime = world.getTimeOfDay() % 24000L;
        boolean inWindow = isWithinDayWindow(dayTime, cfg.breathCondensationStartTick, cfg.breathCondensationEndTick);
        if (!inWindow) return BreathEligibility.deny("condensation window");

        boolean goodTemp = temp > cfg.alwaysBreathTemperature && temp <= cfg.maxBreathCondensationTemperature;
        return goodTemp ? BreathEligibility.allow() : BreathEligibility.deny("temperature");
    }

    public static boolean isEligibleNow(ClientWorld world, PlayerEntity player, ColdBreathConfig cfg) {
        return checkEligibility(world, player, cfg).allowed();
    }

    public record BreathEligibility(boolean allowed, String reason) {
        private static final BreathEligibility ALWAYS = new BreathEligibility(true, null);

        public static BreathEligibility allow() { return ALWAYS; }
        public static BreathEligibility deny(String reason) { return new BreathEligibility(false, reason); }
    }

	public enum DimensionKind {
		OVERWORLD,
		NETHER,
		END,
		OTHER
	}
}


