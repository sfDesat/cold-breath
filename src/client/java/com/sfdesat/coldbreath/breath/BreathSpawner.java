package com.sfdesat.coldbreath.breath;

import com.sfdesat.config.ColdBreathConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class BreathSpawner {

	private BreathSpawner() {}

	public static void spawnAir(Minecraft client, ClientLevel level, Player player, ColdBreathConfig cfg) {
		Vec3 headPos = new Vec3(player.getX(), player.getEyeY(), player.getZ());
		Vec3 look = player.getViewVector(1.0f).normalize();
		Vec3 forward = look.scale(cfg.forwardOffset);
		Vec3 down = new Vec3(0, -cfg.downOffset, 0);
		Vec3 spawn = headPos.add(forward).add(down);

		ThreadLocalRandom r = ThreadLocalRandom.current();

		int count = r.nextInt(3) == 0 ? 2 : 1;
		for (int i = 0; i < count; i++) {
			double ox = (r.nextDouble() - 0.5) * 0.08;
			double oy = (r.nextDouble() - 0.5) * 0.04;
			double oz = (r.nextDouble() - 0.5) * 0.08;

			double vx = look.x * 0.003 + (r.nextDouble() - 0.5) * 0.002;
			double vy = Math.max(0, look.y * 0.001) + (r.nextDouble() - 0.5) * 0.002;
			double vz = look.z * 0.003 + (r.nextDouble() - 0.5) * 0.002;

			DustParticleOptions dust = new DustParticleOptions(cfg.breathColor, (float) Math.max(0.1, cfg.breathSize));
			level.addParticle(dust, spawn.x + ox, spawn.y + oy, spawn.z + oz, vx, vy, vz);
		}
	}

	public static void spawnUnderwater(Minecraft client, ClientLevel level, Player player) {
		Vec3 headPos = new Vec3(player.getX(), player.getEyeY(), player.getZ());
		Vec3 look = player.getViewVector(1.0f).normalize();
		Vec3 forward = look.scale(0.2);
		Vec3 down = new Vec3(0, -0.05, 0);
		Vec3 spawn = headPos.add(forward).add(down);

		ThreadLocalRandom r = ThreadLocalRandom.current();
		int bubbleCount = r.nextInt(2, 4);
		for (int i = 0; i < bubbleCount; i++) {
			double ox = (r.nextDouble() - 0.5) * 0.06;
			double oy = (r.nextDouble() - 0.5) * 0.04;
			double oz = (r.nextDouble() - 0.5) * 0.06;
			double vx = (r.nextDouble() - 0.5) * 0.02;
			double vy = 0.03 + r.nextDouble() * 0.02;
			double vz = (r.nextDouble() - 0.5) * 0.02;
			level.addParticle(ParticleTypes.BUBBLE, spawn.x + ox, spawn.y + oy, spawn.z + oz, vx, vy, vz);
		}
	}
}
