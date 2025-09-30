package com.sfdesat;

import net.fabricmc.api.ModInitializer;
import com.sfdesat.config.ConfigManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColdBreathMod implements ModInitializer {
	public static final String MOD_ID = "coldbreath";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Load config early
		ConfigManager.get();
		LOGGER.info("Cold Breath mod initialized successfully!");
	}
}