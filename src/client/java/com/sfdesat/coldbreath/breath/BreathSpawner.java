package com.sfdesat.coldbreath.breath;

import com.sfdesat.config.ColdBreathConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class BreathSpawner {

	private BreathSpawner() {}

	public static void spawnAir(MinecraftClient client, ClientWorld world, PlayerEntity player, ColdBreathConfig cfg) {
		Vec3d headPos = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
		Vec3d look = player.getRotationVec(1.0f).normalize();
		Vec3d forward = look.multiply(cfg.forwardOffset);
		Vec3d down = new Vec3d(0, -cfg.downOffset, 0);
		Vec3d spawn = headPos.add(forward).add(down);

		ThreadLocalRandom r = ThreadLocalRandom.current();
		ParticleManager pm = client.particleManager;

		int count = r.nextInt(3) == 0 ? 2 : 1;
		for (int i = 0; i < count; i++) {
			double ox = (r.nextDouble() - 0.5) * 0.08;
			double oy = (r.nextDouble() - 0.5) * 0.04;
			double oz = (r.nextDouble() - 0.5) * 0.08;

			double vx = look.x * 0.003 + (r.nextDouble() - 0.5) * 0.002;
			double vy = Math.max(0, look.y * 0.001) + (r.nextDouble() - 0.5) * 0.002;
			double vz = look.z * 0.003 + (r.nextDouble() - 0.5) * 0.002;

			DustParticleEffect dust = VersionChecker.make(cfg.breathColor, (float)Math.max(0.1, cfg.breathSize));
			pm.addParticle(dust, spawn.x + ox, spawn.y + oy, spawn.z + oz, vx, vy, vz);
		}
	}

	public static void spawnUnderwater(MinecraftClient client, ClientWorld world, PlayerEntity player) {
		Vec3d headPos = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
		Vec3d look = player.getRotationVec(1.0f).normalize();
		Vec3d forward = look.multiply(0.2);
		Vec3d down = new Vec3d(0, -0.05, 0);
		Vec3d spawn = headPos.add(forward).add(down);

		ThreadLocalRandom r = ThreadLocalRandom.current();
		ParticleManager pm = client.particleManager;
		int bubbleCount = r.nextInt(2, 4);
		for (int i = 0; i < bubbleCount; i++) {
			double ox = (r.nextDouble() - 0.5) * 0.06;
			double oy = (r.nextDouble() - 0.5) * 0.04;
			double oz = (r.nextDouble() - 0.5) * 0.06;
			double vx = (r.nextDouble() - 0.5) * 0.02;
			double vy = 0.03 + r.nextDouble() * 0.02;
			double vz = (r.nextDouble() - 0.5) * 0.02;
			pm.addParticle(ParticleTypes.BUBBLE, spawn.x + ox, spawn.y + oy, spawn.z + oz, vx, vy, vz);
		}
	}


}


