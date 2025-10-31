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

	public static boolean isEligibleNow(ClientWorld world, PlayerEntity player, ColdBreathConfig cfg) {
		if (!cfg.enabled) return false;
		if (player.isSpectator() || player.isSleeping() || player.isDead()) return false;
		if (!cfg.visibleInCreative && player.getAbilities().creativeMode) return false;

		if (player.isSubmergedInWater()) return cfg.underwaterEnabled;

		DimensionKind dim = getDimensionKind(world);
		if (dim == DimensionKind.NETHER) return cfg.visibleInNether;
		if (dim == DimensionKind.END) return cfg.visibleInEnd;

		BlockPos pos = player.getBlockPos();
		float temp = computeEffectiveTemperature(world, pos, cfg);
		boolean isColdHere = temp <= cfg.alwaysBreathTemperature;
		if (!cfg.onlyInColdBiomes) return true;
		if (isColdHere) return true;

		boolean morningEnabled = SeasonManager.isMorningBreathEnabled(cfg.morningBreathEnabled);
		if (cfg.morningBreathEnabled && morningEnabled) {
			long dayTime = world.getTimeOfDay() % 24000L;
			boolean inWindow = isWithinDayWindow(dayTime, cfg.morningBreathStartTick, cfg.morningBreathEndTick);
			boolean goodTemp = temp > cfg.alwaysBreathTemperature && temp <= cfg.maxMorningBreathTemperature;
			return inWindow && goodTemp;
		}
		return false;
	}

	public enum DimensionKind {
		OVERWORLD,
		NETHER,
		END,
		OTHER
	}
}


