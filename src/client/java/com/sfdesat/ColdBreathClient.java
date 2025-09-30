package com.sfdesat;

import net.fabricmc.api.ClientModInitializer;
import com.sfdesat.client.coldbreath.ColdBreathEffect;

public class ColdBreathClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ColdBreathEffect.register();
	}
}