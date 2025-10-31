package com.sfdesat.coldbreath.breath;

import com.sfdesat.coldbreath.api.ColdBreathApi;
import com.sfdesat.coldbreath.season.SeasonManager;
import com.sfdesat.config.ColdBreathConfig;
import com.sfdesat.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.concurrent.ThreadLocalRandom;

public final class BreathController {

	private static final int TICKS_PER_SECOND = 20;
	private static final int BURST_EMIT_PERIOD_TICKS = 3;
	private static final double INTERVAL_TIE_EPSILON = 1e-6;

	private long nextBreathTick;
	private long breathBurstEndTick;
	private final StateBlends blends;

	public BreathController() {
		this.nextBreathTick = 0L;
		this.breathBurstEndTick = 0L;
		this.blends = new StateBlends();
	}

	public void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> onTick(client));
	}

	private void onTick(MinecraftClient client) {
		ColdBreathConfig cfg = ConfigManager.get();
		if (!cfg.enabled) return;
		if (client.isPaused()) return;
		ClientWorld world = client.world;
		PlayerEntity player = client.player;
		if (world == null || player == null) return;

		long time = world.getTime();
		blends.tick(player, cfg);

		if (time < breathBurstEndTick) {
			if (time % BURST_EMIT_PERIOD_TICKS == 0) {
				if (player.isSubmergedInWater()) {
					if (cfg.underwaterEnabled) BreathSpawner.spawnUnderwater(client, world, player);
				} else {
					BreathSpawner.spawnAir(client, world, player, cfg);
				}
			}
			return;
		}

		if (time < nextBreathTick) return;
		SeasonManager.refresh(world);

        if (!EnvModel.isEligibleNow(world, player, cfg)) {
			scheduleNext(time, cfg);
			return;
		}

		// Underwater path: only underwater interval should be respected
		if (player.isSubmergedInWater() && cfg.underwaterEnabled) {
			startBurst(time, cfg);
			scheduleNextUnderwater(time, cfg);
            ColdBreathApi.publishBreathEvent();
			return;
		}

		// Air path: use normal interval logic (sprint/health blending)
		startBurst(time, cfg);
		scheduleNext(time, cfg);
        ColdBreathApi.publishBreathEvent();
	}

	private void startBurst(long now, ColdBreathConfig cfg) {
		breathBurstEndTick = now + cfg.breathBurstDurationTicks;
	}

	private void scheduleNext(long nowTick, ColdBreathConfig cfg) {
		double baseNormal = Math.max(0.1, cfg.baseIntervalSeconds);
		double devNormal = Math.max(0.0, cfg.intervalDeviationSeconds);
		double baseSprint = Math.max(0.1, cfg.sprintBaseIntervalSeconds);
		double devSprint = Math.max(0.0, cfg.sprintIntervalDeviationSeconds);
		double baseHealth = Math.max(0.1, cfg.lowHealthIntervalSeconds);
		double devHealth = Math.max(0.0, cfg.healthIntervalDeviationSeconds);

		double sprintT = (cfg.sprintingIntervalsEnabled ? blends.getSprintBlend() : 0.0);
		double healthT = (cfg.healthBasedBreathingEnabled ? blends.getHealthBlend() : 0.0);

		double afterSprint = lerp(baseNormal, baseSprint, sprintT);
		double devAfterSprint = lerp(devNormal, devSprint, sprintT);
		double afterHealth = lerp(baseNormal, baseHealth, healthT);
		double devAfterHealth = lerp(devNormal, devHealth, healthT);

		double base;
		double dev;
		if (Math.abs(afterSprint - afterHealth) <= INTERVAL_TIE_EPSILON) {
			base = afterSprint; // equal enough
			dev = 0.5 * (devAfterSprint + devAfterHealth);
		} else if (afterSprint < afterHealth) {
			base = afterSprint;
			dev = devAfterSprint;
		} else {
			base = afterHealth;
			dev = devAfterHealth;
		}

		double minSec = Math.max(0.1, base - dev);
		double maxSec = Math.max(minSec, base + dev);
		double waitSec = (maxSec <= minSec) ? minSec : ThreadLocalRandom.current().nextDouble(minSec, maxSec);
		int waitTicks = Math.max(1, (int)Math.round(waitSec * TICKS_PER_SECOND));
		this.nextBreathTick = nowTick + waitTicks;
	}

	private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

	private void scheduleNextUnderwater(long nowTick, ColdBreathConfig cfg) {
		double base = Math.max(0.1, cfg.underwaterBaseIntervalSeconds);
		double dev = Math.max(0.0, cfg.underwaterIntervalDeviationSeconds);
		double minSec = Math.max(0.1, base - dev);
		double maxSec = Math.max(minSec, base + dev);
		double waitSec = (maxSec <= minSec) ? minSec : ThreadLocalRandom.current().nextDouble(minSec, maxSec);
		int waitTicks = Math.max(1, (int)Math.round(waitSec * TICKS_PER_SECOND));
		this.nextBreathTick = nowTick + waitTicks;
	}

	public double getCurrentBaseIntervalSeconds(ColdBreathConfig cfg) {
		double baseNormal = Math.max(0.1, cfg.baseIntervalSeconds);
		double baseSprint = Math.max(0.1, cfg.sprintBaseIntervalSeconds);
		double baseHealth = Math.max(0.1, cfg.lowHealthIntervalSeconds);
		double sprintT = (cfg.sprintingIntervalsEnabled ? blends.getSprintBlend() : 0.0);
		double healthT = (cfg.healthBasedBreathingEnabled ? blends.getHealthBlend() : 0.0);
		double afterSprint = lerp(baseNormal, baseSprint, sprintT);
		double afterHealth = lerp(baseNormal, baseHealth, healthT);
		return Math.min(afterSprint, afterHealth);
	}

	public StateBlends getBlends() { return blends; }

	public double[] getCurrentIntervalMinMaxSeconds(ColdBreathConfig cfg) {
		double baseNormal = Math.max(0.1, cfg.baseIntervalSeconds);
		double devNormal = Math.max(0.0, cfg.intervalDeviationSeconds);
		double baseSprint = Math.max(0.1, cfg.sprintBaseIntervalSeconds);
		double devSprint = Math.max(0.0, cfg.sprintIntervalDeviationSeconds);
		double baseHealth = Math.max(0.1, cfg.lowHealthIntervalSeconds);
		double devHealth = Math.max(0.0, cfg.healthIntervalDeviationSeconds);

		double sprintT = (cfg.sprintingIntervalsEnabled ? blends.getSprintBlend() : 0.0);
		double healthT = (cfg.healthBasedBreathingEnabled ? blends.getHealthBlend() : 0.0);

		double afterSprint = lerp(baseNormal, baseSprint, sprintT);
		double devAfterSprint = lerp(devNormal, devSprint, sprintT);
		double afterHealth = lerp(baseNormal, baseHealth, healthT);
		double devAfterHealth = lerp(devNormal, devHealth, healthT);

		double base;
		double dev;
		if (Math.abs(afterSprint - afterHealth) <= INTERVAL_TIE_EPSILON) {
			base = afterSprint;
			dev = 0.5 * (devAfterSprint + devAfterHealth);
		} else if (afterSprint < afterHealth) {
			base = afterSprint;
			dev = devAfterSprint;
		} else {
			base = afterHealth;
			dev = devAfterHealth;
		}

		double minSec = Math.max(0.1, base - dev);
		double maxSec = Math.max(minSec, base + dev);
		return new double[] { minSec, maxSec };
	}

	public static final BreathController INSTANCE = new BreathController();
}


