package com.sfdesat.client.coldbreath;

import com.sfdesat.config.ColdBreathConfig;
import com.sfdesat.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.particle.ParticleManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a small "breath" puff in front of the player at random intervals,
 * client-side only, and only in cold biomes.
 */
public final class ColdBreathEffect {

    private static long nextBreathTick = 0;
    private static long breathBurstEndTick = 0;
    private static double sprintBlend = 0.0; // 0.0 normal, 1.0 sprinting
    private static double prevSprintBlend = 0.0;

    private ColdBreathEffect() { }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ColdBreathConfig cfg = ConfigManager.get();
            if (!cfg.enabled) return;
            if (client.isPaused()) return;
            ClientWorld world = client.world;
            PlayerEntity player = client.player;
            if (world == null || player == null) return;

            long time = world.getTime();

            // Update sprint blending EVERY tick so HUD reflects state immediately
            prevSprintBlend = sprintBlend;
            updateSprintBlend(player, cfg);

            if (time < breathBurstEndTick) {
                if (time % 3 == 0) {
                    if (player.isSubmergedInWater()) {
                        if (cfg.underwaterEnabled) {
                            spawnUnderwaterBreath(client, world, player);
                        }
                    } else {
                        spawnBreathParticles(client, world, player, cfg);
                    }
                }
                return;
            }

            if (time < nextBreathTick) return;

            // Underwater path (overrides sprinting logic)
            if (player.isSubmergedInWater()) {
                if (!cfg.underwaterEnabled) {
                    scheduleNext(time, cfg);
                    return;
                }
                startBreathBurst(time, cfg);
                scheduleNextUnderwater(time, cfg);
                return;
            }

            if (player.isSpectator() || player.isSleeping()) {
                scheduleNext(time, cfg);
                return;
            }

            if (!cfg.visibleInCreative && player.getAbilities().creativeMode) {
                scheduleNext(time, cfg);
                return;
            }

            BlockPos pos = player.getBlockPos();
            Biome biome = world.getBiome(pos).value();
            int seaLevel = world.getSeaLevel();
            boolean isColdHere = biome.isCold(pos, seaLevel);

            // Dimension visibility gating
            String dimKey = world.getRegistryKey().getValue().toString();
            boolean isNether = dimKey.contains("nether");
            boolean isEnd = dimKey.contains("the_end") || dimKey.contains("end");
            if ((isNether && !cfg.visibleInNether) || (isEnd && !cfg.visibleInEnd)) {
                scheduleNext(time, cfg);
                return;
            }

            if (cfg.onlyInColdBiomes && !isColdHere) {
                scheduleNext(time, cfg);
                return;
            }

            startBreathBurst(time, cfg);
            scheduleNext(time, cfg);
        });

        // HUD debug overlay
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            ColdBreathConfig cfg = ConfigManager.get();
            if (!cfg.debugEnabled) return;
            int color = getDebugColor(cfg);
            int x = 6;
            int y = 6;
            int w = 80;
            int h = 12;
            context.fill(x, y, x + w, y + h, color);

            // State text inside rectangle
            String state = getDebugState(cfg);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                context.drawText(client.textRenderer, Text.literal(state), x + 3, y + 2, 0xFFFFFFFF, false);
                // Blend numbers next to rectangle
                String blendText = String.format("blend: %.2f -> %.2f", prevSprintBlend, sprintBlend);
                context.drawText(client.textRenderer, Text.literal(blendText), x + w + 6, y + 2, 0xFFFFFF00, false);
            }
        });
    }

    private static void startBreathBurst(long currentTick, ColdBreathConfig cfg) {
        breathBurstEndTick = currentTick + cfg.breathBurstDurationTicks;
    }

    private static void spawnBreathParticles(MinecraftClient client, ClientWorld world, PlayerEntity player, ColdBreathConfig cfg) {
        Vec3d headPos = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
        Vec3d lookDirection = player.getRotationVec(1.0f).normalize();

        Vec3d forward = lookDirection.multiply(cfg.forwardOffset);
        Vec3d down = new Vec3d(0, -cfg.downOffset, 0);

        Vec3d spawn = headPos.add(forward).add(down);

        ThreadLocalRandom r = ThreadLocalRandom.current();
        ParticleManager particleManager = client.particleManager;

        int snowflakeCount = r.nextInt(3) == 0 ? 2 : 1;
        for (int i = 0; i < snowflakeCount; i++) {
            double offsetX = (r.nextDouble() - 0.5) * 0.08;
            double offsetY = (r.nextDouble() - 0.5) * 0.04;
            double offsetZ = (r.nextDouble() - 0.5) * 0.08;

            double vx = lookDirection.x * 0.003 + (r.nextDouble() - 0.5) * 0.002;
            double vy = Math.max(0, lookDirection.y * 0.001) + (r.nextDouble() - 0.5) * 0.002;
            double vz = lookDirection.z * 0.003 + (r.nextDouble() - 0.5) * 0.002;

            int color = cfg.breathColor;
            float size = (float)Math.max(0.1, cfg.breathSize);
            DustParticleEffect dustEffect = new DustParticleEffect(color, size);

            particleManager.addParticle(dustEffect,
                spawn.x + offsetX, spawn.y + offsetY, spawn.z + offsetZ, vx, vy, vz);
        }
    }

    private static void spawnUnderwaterBreath(MinecraftClient client, ClientWorld world, PlayerEntity player) {
        Vec3d headPos = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
        Vec3d lookDirection = player.getRotationVec(1.0f).normalize();

        Vec3d forward = lookDirection.multiply(0.2);
        Vec3d down = new Vec3d(0, -0.05, 0);
        Vec3d spawn = headPos.add(forward).add(down);

        ThreadLocalRandom r = ThreadLocalRandom.current();
        ParticleManager particleManager = client.particleManager;

        int bubbleCount = r.nextInt(2, 4);
        for (int i = 0; i < bubbleCount; i++) {
            double offsetX = (r.nextDouble() - 0.5) * 0.06;
            double offsetY = (r.nextDouble() - 0.5) * 0.04;
            double offsetZ = (r.nextDouble() - 0.5) * 0.06;

            double vx = (r.nextDouble() - 0.5) * 0.02;
            double vy = 0.03 + r.nextDouble() * 0.02; // rising bubbles
            double vz = (r.nextDouble() - 0.5) * 0.02;

            particleManager.addParticle(ParticleTypes.BUBBLE,
                spawn.x + offsetX, spawn.y + offsetY, spawn.z + offsetZ, vx, vy, vz);
        }
    }

    private static void scheduleNext(long nowTick, ColdBreathConfig cfg) {
        // Determine blended intervals (normal <-> sprint) based on sprintBlend
        double baseNormal = Math.max(0.1, cfg.baseIntervalSeconds);
        double devNormal = Math.max(0.0, cfg.intervalDeviationSeconds);

        double baseSprint = Math.max(0.1, cfg.sprintBaseIntervalSeconds);
        double devSprint = Math.max(0.0, cfg.sprintIntervalDeviationSeconds);

        double blend = (cfg.sprintingIntervalsEnabled ? sprintBlend : 0.0);
        double base = lerp(baseNormal, baseSprint, blend);
        double dev = lerp(devNormal, devSprint, blend);

        double minSec = Math.max(0.1, base - dev);
        double maxSec = Math.max(minSec, base + dev);
        double waitSec;
        if (maxSec <= minSec) {
            waitSec = minSec;
        } else {
            waitSec = ThreadLocalRandom.current().nextDouble(minSec, maxSec);
        }
        int waitTicks = Math.max(1, (int)Math.round(waitSec * 20.0));
        nextBreathTick = nowTick + waitTicks;
    }

    private static void scheduleNextUnderwater(long nowTick, ColdBreathConfig cfg) {
        double base = Math.max(0.1, cfg.underwaterBaseIntervalSeconds);
        double dev = Math.max(0.0, cfg.underwaterIntervalDeviationSeconds);
        // Clamp to [4, 20] seconds as requested
        double minSec = Math.max(4.0, base - dev);
        double maxSec = Math.min(20.0, Math.max(minSec, base + dev));
        double waitSec = (maxSec <= minSec) ? minSec : ThreadLocalRandom.current().nextDouble(minSec, maxSec);
        int waitTicks = Math.max(1, (int)Math.round(waitSec * 20.0));
        nextBreathTick = nowTick + waitTicks;
    }

    private static void updateSprintBlend(PlayerEntity player, ColdBreathConfig cfg) {
        boolean underwater = player.isSubmergedInWater();
        double dt = 1.0 / 20.0; // seconds per tick
        double upRate = cfg.sprintBuildUpSeconds <= 0 ? 1.0 : dt / cfg.sprintBuildUpSeconds;
        double downRate = cfg.sprintBuildDownSeconds <= 0 ? 1.0 : dt / cfg.sprintBuildDownSeconds;

        // Underwater: always build up towards 1.0 (out-of-breath effect builds while submerged)
        if (underwater && cfg.underwaterEnabled) {
            if (sprintBlend < 1.0) {
                sprintBlend = Math.min(1.0, sprintBlend + upRate);
            }
            return;
        }

        // Out of water: normal sprint behavior
        boolean sprinting = player.isSprinting();
        double target = (cfg.sprintingIntervalsEnabled && sprinting) ? 1.0 : 0.0;
        if (target > sprintBlend) {
            sprintBlend = Math.min(1.0, sprintBlend + upRate);
        } else if (target < sprintBlend) {
            sprintBlend = Math.max(0.0, sprintBlend - downRate);
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int getDebugColor(ColdBreathConfig cfg) {
        // State colors (ARGB):
        // Green: standing still/normal (blend ~0)
        // Yellow: building up (blend increasing)
        // Orange: full sprinting (blend ~1)
        // Blue: building down (blend decreasing)
        int alpha = 0xA0 << 24; // ~63% opacity
        if (!cfg.sprintingIntervalsEnabled) {
            return alpha | 0x00FF00; // green
        }
        if (sprintBlend >= 0.95) {
            return alpha | 0xFFA500; // orange
        }
        if (sprintBlend <= 0.05) {
            return alpha | 0x00FF00; // green
        }
        // Determine direction from previous blend
        if (sprintBlend > prevSprintBlend + 1e-6) {
            return alpha | 0xFFFF00; // yellow building up
        } else if (sprintBlend < prevSprintBlend - 1e-6) {
            return alpha | 0x0000FF; // blue building down
        } else {
            // Stable intermediate (rare), indicate as yellowish-green
            return alpha | 0x7FFF00;
        }
    }

    private static String getDebugState(ColdBreathConfig cfg) {
        if (!cfg.sprintingIntervalsEnabled) return "normal";
        if (sprintBlend >= 0.95) return "sprinting";
        if (sprintBlend <= 0.05) return "normal";
        if (sprintBlend > prevSprintBlend + 1e-6) return "building up";
        if (sprintBlend < prevSprintBlend - 1e-6) return "building down";
        return "transitional";
    }
}
