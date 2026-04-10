package com.sfdesat.coldbreath.breath;

import com.sfdesat.coldbreath.season.SeasonManager;
import com.sfdesat.config.ColdBreathConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class EnvModel {

	private EnvModel() {}

	public static float computeEffectiveTemperature(ClientLevel level, BlockPos pos, ColdBreathConfig cfg) {
		Biome biome = level.getBiome(pos).value();
		float baseTemperature = biome.getBaseTemperature();
		float temperature = baseTemperature;
		if (cfg.altitudeAdjustmentEnabled) {
			int seaLevel = level.getSeaLevel();
			int altitude = pos.getY();
			temperature = baseTemperature - (altitude - seaLevel) * (float) cfg.altitudeTemperatureRate;
		}
		temperature += (float) SeasonManager.getTemperatureOffset();
		return temperature;
	}

	public static boolean isWithinDayWindow(long dayTime, long start, long end) {
		if (start == end) return false;
		return (start <= end) ? (dayTime >= start && dayTime <= end)
			: (dayTime >= start || dayTime <= end);
	}

	public static DimensionKind getDimensionKind(ClientLevel level) {
		if (level.dimension() == Level.OVERWORLD) return DimensionKind.OVERWORLD;
		if (level.dimension() == Level.NETHER) return DimensionKind.NETHER;
		if (level.dimension() == Level.END) return DimensionKind.END;
		Identifier id = level.dimension().identifier();
		String path = id.getPath();
		if (path.contains("nether")) return DimensionKind.NETHER;
		if (path.contains("end") || path.contains("the_end")) return DimensionKind.END;
		return DimensionKind.OTHER;
	}

	public static BreathEligibility checkEligibility(ClientLevel level, Player player, ColdBreathConfig cfg) {
		if (!cfg.enabled) return BreathEligibility.deny("disabled");
		if (player.isSpectator()) return BreathEligibility.deny("spectator");
		if (player.isSleeping()) return BreathEligibility.deny("sleeping");
		if (player.isDeadOrDying()) return BreathEligibility.deny("dead");
		if (!cfg.visibleInCreative && player.isCreative()) return BreathEligibility.deny("creative");

		if (player.isUnderWater()) {
			return cfg.underwaterEnabled
					? BreathEligibility.allow()
					: BreathEligibility.deny("underwater");
		}

		DimensionKind dim = getDimensionKind(level);
		if (dim == DimensionKind.NETHER && !cfg.visibleInNether) return BreathEligibility.deny("nether hidden");
		if (dim == DimensionKind.END && !cfg.visibleInEnd) return BreathEligibility.deny("end hidden");

		BlockPos pos = player.blockPosition();
		float temp = computeEffectiveTemperature(level, pos, cfg);
		boolean isColdHere = temp <= cfg.alwaysBreathTemperature;
		if (cfg.alwaysShowBreath || isColdHere) return BreathEligibility.allow();

		if (!cfg.breathCondensationEnabled) return BreathEligibility.deny("condensation off");

		if (!SeasonManager.isBreathCondensationEnabled(true)) {
			return BreathEligibility.deny("season");
		}

		long dayTime = level.getGameTime() % 24000L;
		boolean inWindow = isWithinDayWindow(dayTime, cfg.breathCondensationStartTick, cfg.breathCondensationEndTick);
		if (!inWindow) return BreathEligibility.deny("condensation window");

		boolean goodTemp = temp > cfg.alwaysBreathTemperature && temp <= cfg.maxBreathCondensationTemperature;
		return goodTemp ? BreathEligibility.allow() : BreathEligibility.deny("temperature");
	}

	public static boolean isEligibleNow(ClientLevel level, Player player, ColdBreathConfig cfg) {
		return checkEligibility(level, player, cfg).allowed();
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
