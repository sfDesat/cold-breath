package com.sfdesat.config;

import com.sfdesat.ColdBreathMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ColdBreathConfig cached = null;

	private ConfigManager() {}

	public static ColdBreathConfig get() {
		if (cached == null) {
			cached = load();
		}
		return cached;
	}

	public static ColdBreathConfig load() {
		Path path = getConfigPath();
		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				ColdBreathConfig cfg = GSON.fromJson(reader, ColdBreathConfig.class);
				if (cfg != null) return cfg;
			} catch (IOException ignored) {}
		}
		return new ColdBreathConfig();
	}

	public static void save() {
		if (cached == null) return;
		Path path = getConfigPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(cached, writer);
			}
		} catch (IOException e) {
			ColdBreathMod.LOGGER.warn("Failed to save config", e);
		}
	}

	private static Path getConfigPath() {
		return Path.of("config", ColdBreathMod.MOD_ID + ".json");
	}
}


