package com.sfdesat.coldbreath.breath;

import com.sfdesat.config.ColdBreathConfig;
import net.minecraft.entity.player.PlayerEntity;

public final class StateBlends {

	private double sprintBlend;
	private double prevSprintBlend;
	private double healthBlend;
	private double prevHealthBlend;

	public StateBlends() {
		this.sprintBlend = 0.0;
		this.prevSprintBlend = 0.0;
		this.healthBlend = 0.0;
		this.prevHealthBlend = 0.0;
	}

	public void tick(PlayerEntity player, ColdBreathConfig cfg) {
		prevSprintBlend = sprintBlend;
		prevHealthBlend = healthBlend;
		updateSprintBlend(player, cfg);
		updateHealthBlend(player, cfg);
	}

	private void updateSprintBlend(PlayerEntity player, ColdBreathConfig cfg) {
		boolean underwater = player.isSubmergedInWater();
		double dt = 1.0 / 20.0;
		double upRate = cfg.sprintBuildUpSeconds <= 0 ? 1.0 : dt / cfg.sprintBuildUpSeconds;
		double downRate = cfg.sprintBuildDownSeconds <= 0 ? 1.0 : dt / cfg.sprintBuildDownSeconds;

		if (underwater && cfg.underwaterEnabled) {
			if (sprintBlend < 1.0) sprintBlend = Math.min(1.0, sprintBlend + upRate);
			return;
		}

		boolean sprinting = player.isSprinting();
		double target = (cfg.sprintingIntervalsEnabled && sprinting) ? 1.0 : 0.0;
		if (target > sprintBlend) sprintBlend = Math.min(1.0, sprintBlend + upRate);
		else if (target < sprintBlend) sprintBlend = Math.max(0.0, sprintBlend - downRate);
	}

	private void updateHealthBlend(PlayerEntity player, ColdBreathConfig cfg) {
		if (!cfg.healthBasedBreathingEnabled) {
			healthBlend = 0.0;
			return;
		}
		float maxHealth = player.getMaxHealth();
		float currentHealth = player.getHealth();
		float healthPercentage = 1.0f - (currentHealth / maxHealth);
		double target = Math.max(0.0, Math.min(1.0, healthPercentage));
		// exponential smoothing
		double alpha = 0.2; // response speed
		healthBlend = healthBlend + alpha * (target - healthBlend);
	}

	public double getSprintBlend() { return sprintBlend; }
	public double getPrevSprintBlend() { return prevSprintBlend; }
	public double getHealthBlend() { return healthBlend; }
	public double getPrevHealthBlend() { return prevHealthBlend; }
}


