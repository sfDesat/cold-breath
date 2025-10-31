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
        new DebugHud(debugManager).register();
        new DebugChat(debugManager).register();
    }
}