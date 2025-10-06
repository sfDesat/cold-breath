package com.sfdesat.coldbreath.debug;

import com.sfdesat.coldbreath.breath.EnvModel;
import com.sfdesat.coldbreath.breath.StateBlends;
import com.sfdesat.config.ColdBreathConfig;
import com.sfdesat.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class BreathDebugHud {

	private final StateBlends blends;

	public BreathDebugHud(StateBlends blends) {
		this.blends = blends;
	}

	public void register() {
		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			ColdBreathConfig cfg = ConfigManager.get();
			if (!cfg.debugEnabled) return;
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null || client.textRenderer == null) return;

			int x = 6;
			int y = 6;
			ClientWorld world = client.world;

			// Background panel (very light, transparent)
			int lines = 8; // total lines we draw below
			int lineHeight = 12;
			int padding = 10; // a bit more padding to extend vertically
			int bgWidth = 260; // slightly narrower background
			int bgHeight = lines * lineHeight + padding;
			int bgColor = 0x66000000; // dark gray/black with high transparency
			context.fill(x - 3, y - 3, x - 3 + bgWidth, y - 3 + bgHeight, bgColor);

			// 1) Breathing: true | false | bubbles
			String breathingText = "breathing: unknown";
			int breathingColor = 0xFFFFFFFF;
			if (world != null && client.player != null) {
				boolean underwater = client.player.isSubmergedInWater();
				if (underwater && cfg.underwaterEnabled) {
					breathingText = "breathing: bubbles";
					breathingColor = 0xFF4EA3FF; // blue
				} else {
					boolean eligible = EnvModel.isEligibleNow(world, client.player, cfg);
					breathingText = eligible ? "breathing: true" : "breathing: false";
					breathingColor = eligible ? 0xFF00FF00 : 0xFFFF0000; // green / red
				}
			}
			context.drawText(client.textRenderer, Text.literal(breathingText), x, y, breathingColor, false);
			y += 12;

			// 2) Current breathing interval (first the real-time interval)
			double baseInterval = com.sfdesat.coldbreath.breath.BreathController.INSTANCE.getCurrentBaseIntervalSeconds(cfg);
			if (world != null && client.player != null && client.player.isSubmergedInWater() && cfg.underwaterEnabled) baseInterval = Math.max(0.1, cfg.underwaterBaseIntervalSeconds);
			context.drawText(client.textRenderer, Text.literal(String.format("interval: %.1fs", baseInterval)), x, y, 0xFFFFFFFF, false);
			y += 12;

			// Then min/max interval accounting for deviation
			double minInt = baseInterval;
			double maxInt = baseInterval;
			if (world != null && client.player != null && client.player.isSubmergedInWater() && cfg.underwaterEnabled) {
				// underwater dedicated range
				double base = Math.max(0.1, cfg.underwaterBaseIntervalSeconds);
				double dev = Math.max(0.0, cfg.underwaterIntervalDeviationSeconds);
				minInt = Math.max(0.1, base - dev);
				maxInt = Math.max(minInt, base + dev);
			} else {
				double[] range = com.sfdesat.coldbreath.breath.BreathController.INSTANCE.getCurrentIntervalMinMaxSeconds(cfg);
				minInt = range[0];
				maxInt = range[1];
			}
			context.drawText(client.textRenderer, Text.literal(String.format("min/max: %.1fs / %.1fs", minInt, maxInt)), x, y, 0xFFFFFFFF, false);
			y += 12;

			// 3) Temp, base temp, altitude above sea level
			if (world != null && client.player != null) {
				BlockPos pos = client.player.getBlockPos();
				float baseTemp = world.getBiome(pos).value().getTemperature();
				float effTemp = EnvModel.computeEffectiveTemperature(world, pos, cfg);
				int sea = world.getSeaLevel();
				int alt = pos.getY() - sea;
				String tempLine = String.format("temp: %.3f (base: %.3f), alt: %+d", effTemp, baseTemp, alt);
				context.drawText(client.textRenderer, Text.literal(tempLine), x, y, 0xFFFFFFFF, false);
				y += 12;
			}

			// 4) Status and sprint/health build up
			String status = getDebugState(cfg);
			String blendsLine = String.format("status: %s | sprint: %.2f | health: %.2f", status, blends.getSprintBlend(), blends.getHealthBlend());
			context.drawText(client.textRenderer, Text.literal(blendsLine), x, y, 0xFFFFFFFF, false);
			y += 12;

			// 5) Morning breath: true | false
			if (world != null && client.player != null) {
				long dayTime = world.getTimeOfDay() % 24000L;
				boolean inWindow = EnvModel.isWithinDayWindow(dayTime, cfg.morningBreathStartTick, cfg.morningBreathEndTick);
				float temp = EnvModel.computeEffectiveTemperature(world, client.player.getBlockPos(), cfg);
				boolean okTemp = temp > cfg.alwaysBreathTemperature && temp <= cfg.maxMorningBreathTemperature;
				boolean morningActive = cfg.morningBreathEnabled && inWindow && okTemp;
				int morningColor = morningActive ? 0xFF00FF00 : 0xFFFF0000;
				context.drawText(client.textRenderer, Text.literal("morning breath: " + (morningActive ? "true" : "false")), x, y, morningColor, false);
				y += 12;

				// 6) Time and morning range
				String range = String.format("time: %d | morning range: %d-%d", dayTime, cfg.morningBreathStartTick, cfg.morningBreathEndTick);
				context.drawText(client.textRenderer, Text.literal(range), x, y, 0xFFFFFFFF, false);
				y += 12;
			}

			// 7) Dimension
			String dimText = "dim: unknown";
			if (world != null) {
				EnvModel.DimensionKind kind = EnvModel.getDimensionKind(world);
				dimText = "dim: " + kind.name().toLowerCase() + " (" + world.getRegistryKey().getValue() + ")";
			}
			context.drawText(client.textRenderer, Text.literal(dimText), x, y, 0xFFFFFFFF, false);
		});
	}

	private String getDebugState(ColdBreathConfig cfg) {
		if (!cfg.sprintingIntervalsEnabled && !cfg.healthBasedBreathingEnabled) return "normal";
		double baseNormal = Math.max(0.1, cfg.baseIntervalSeconds);
		double baseSprint = Math.max(0.1, cfg.sprintBaseIntervalSeconds);
		double baseHealth = Math.max(0.1, cfg.lowHealthIntervalSeconds);
		double afterSprint = lerp(baseNormal, baseSprint, cfg.sprintingIntervalsEnabled ? blends.getSprintBlend() : 0.0);
		double afterHealth = lerp(baseNormal, baseHealth, cfg.healthBasedBreathingEnabled ? blends.getHealthBlend() : 0.0);
		boolean healthCtrl = cfg.healthBasedBreathingEnabled && afterHealth <= afterSprint;
		boolean sprintCtrl = cfg.sprintingIntervalsEnabled && afterSprint < afterHealth;
		if (healthCtrl) {
			if (blends.getHealthBlend() >= 0.7) return "critical health";
			if (blends.getHealthBlend() >= 0.3) return "low health";
			return "health priority";
		}
		if (sprintCtrl) {
			if (blends.getSprintBlend() >= 0.95) return "sprinting";
			if (blends.getSprintBlend() <= 0.05) return "normal";
			if (blends.getSprintBlend() > blends.getPrevSprintBlend() + 1e-6) return "building up";
			if (blends.getSprintBlend() < blends.getPrevSprintBlend() - 1e-6) return "building down";
			return "transitional";
		}
		return "normal";
	}

	private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}


