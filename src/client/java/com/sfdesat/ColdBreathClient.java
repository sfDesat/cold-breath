package com.sfdesat;

import net.fabricmc.api.ClientModInitializer;
import com.sfdesat.coldbreath.breath.BreathController;
import com.sfdesat.coldbreath.debug.BreathDebugHud;

public class ColdBreathClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BreathController.INSTANCE.register();
        new BreathDebugHud(BreathController.INSTANCE.getBlends()).register();
    }
}