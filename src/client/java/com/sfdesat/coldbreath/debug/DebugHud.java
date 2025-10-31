package com.sfdesat.coldbreath.debug;

import com.sfdesat.ColdBreathMod;
import com.sfdesat.coldbreath.debug.DebugManager.DebugLine;
import com.sfdesat.coldbreath.debug.DebugManager.DebugSnapshot;
import com.sfdesat.config.ColdBreathConfig;
import com.sfdesat.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

public final class DebugHud {

    private final DebugManager manager;

    private boolean incompatibleHudDetected;
    private boolean compatibilityLogged;
    private boolean messageShownForCurrentContext;
    private boolean messagePending;
    private ClientWorld lastWorld;
    private boolean lastDebugEnabled;

    public DebugHud(DebugManager manager) {
        this.manager = manager;
    }

    @SuppressWarnings("deprecation")
    public void register() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            ColdBreathConfig cfg = ConfigManager.get();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;

            boolean debugEnabled = cfg.debugEnabled;

            ClientWorld currentWorld = client.world;
            if (currentWorld != lastWorld) {
                lastWorld = currentWorld;
                if (incompatibleHudDetected && debugEnabled) {
                    messageShownForCurrentContext = false;
                    messagePending = true;
                }
            }

            if (!lastDebugEnabled && debugEnabled && incompatibleHudDetected) {
                messageShownForCurrentContext = false;
                messagePending = true;
            }
            lastDebugEnabled = debugEnabled;

            if (incompatibleHudDetected) {
                sendCompatibilityMessageIfNeeded(client, debugEnabled);
                return;
            }

            if (!debugEnabled) return;
            if (client.textRenderer == null) return;

            try {
                DebugSnapshot snapshot = manager.capture();
                if (snapshot.isEmpty()) return;

                int x = 6;
                int y = 6;
                int lineHeight = 12;
                int padding = 10;
                int bgWidth = 260;
                int lineCount = snapshot.totalLineCount();
                if (lineCount <= 0) return;

                int bgHeight = lineCount * lineHeight + padding;
                int bgColor = 0x66000000;
                context.fill(x - 3, y - 3, x - 3 + bgWidth, y - 3 + bgHeight, bgColor);

                for (DebugLine line : snapshot.lines()) {
                    context.drawText(client.textRenderer, Text.literal(line.text()), x, y, line.color(), false);
                    y += lineHeight;
                }
            } catch (Throwable t) {
                incompatibleHudDetected = true;
                if (!compatibilityLogged) {
                    ColdBreathMod.LOGGER.warn("Cold Breath debug HUD disabled due to a rendering incompatibility", t);
                    compatibilityLogged = true;
                }
                messageShownForCurrentContext = false;
                if (debugEnabled) {
                    messagePending = true;
                    sendCompatibilityMessageIfNeeded(client, debugEnabled);
                }
            }
        });
    }

    private void sendCompatibilityMessageIfNeeded(MinecraftClient client, boolean debugEnabled) {
        if (!incompatibleHudDetected || !debugEnabled) return;
        if (messageShownForCurrentContext && !messagePending) return;
        if (client.player == null) {
            messagePending = true;
            return;
        }

        client.player.sendMessage(Text.literal("[Cold Breath] Debug HUD disabled for compatibility. Minecraft 1.21.3 and older do not support the HUD; try the debug commands with /coldbreath help."), false);
        messageShownForCurrentContext = true;
        messagePending = false;
    }
}

