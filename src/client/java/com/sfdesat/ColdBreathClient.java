package com.sfdesat;

import com.sfdesat.coldbreath.breath.BreathController;
import com.sfdesat.coldbreath.debug.DebugChat;
import com.sfdesat.coldbreath.debug.DebugHud;
import com.sfdesat.coldbreath.debug.DebugManager;
import com.sfdesat.coldbreath.season.SeasonDetector;
import com.sfdesat.coldbreath.season.SeasonManager;
import com.sfdesat.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;

public class ColdBreathClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SeasonDetector.init();
        SeasonManager.applyConfig(ConfigManager.get());
        BreathController.INSTANCE.register();

        DebugManager debugManager = new DebugManager(BreathController.INSTANCE.getBlends());

        try {
            new DebugHud(debugManager).register();
        } catch (LinkageError err) {
            ColdBreathMod.LOGGER.warn("Cold Breath debug HUD disabled due to missing client rendering classes: {}", err.toString());
        } catch (RuntimeException err) {
            ColdBreathMod.LOGGER.warn("Cold Breath debug HUD failed to initialize; debug overlay will be unavailable.", err);
        }

        try {
            new DebugChat(debugManager).register();
        } catch (LinkageError err) {
            ColdBreathMod.LOGGER.warn("Cold Breath debug chat commands disabled due to missing client command API: {}", err.toString());
        } catch (RuntimeException err) {
            ColdBreathMod.LOGGER.warn("Cold Breath debug chat commands failed to initialize.", err);
        }
    }
}