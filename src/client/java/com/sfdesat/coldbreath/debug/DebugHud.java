package com.sfdesat.coldbreath.debug;

import com.sfdesat.ColdBreathMod;
import com.sfdesat.coldbreath.debug.DebugManager.DebugLine;
import com.sfdesat.coldbreath.debug.DebugManager.DebugSnapshot;
import com.sfdesat.config.ColdBreathConfig;
import com.sfdesat.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class DebugHud {

    private final DebugManager manager;

    private boolean incompatibleHudDetected;
    private boolean compatibilityLogged;
    private boolean messageShownForCurrentContext;
    private boolean messagePending;
    private ClientLevel lastWorld;
    private boolean lastDebugEnabled;

    public DebugHud(DebugManager manager) {
        this.manager = manager;
    }

    public void register() {
        Identifier id = Identifier.fromNamespaceAndPath(ColdBreathMod.MOD_ID, "debug_hud");
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id, this::extractRenderState);
    }

    private void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        ColdBreathConfig cfg = ConfigManager.get();
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        boolean debugEnabled = cfg.debugEnabled;

        ClientLevel currentWorld = client.level;
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
        if (client.font == null) return;

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
            graphics.fill(x - 3, y - 3, x - 3 + bgWidth, y - 3 + bgHeight, bgColor);

            for (DebugLine line : snapshot.lines()) {
                graphics.text(client.font, Component.literal(line.text()), x, y, line.color(), false);
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
    }

    private void sendCompatibilityMessageIfNeeded(Minecraft client, boolean debugEnabled) {
        if (!incompatibleHudDetected || !debugEnabled) return;
        if (messageShownForCurrentContext && !messagePending) return;
        if (client.player == null) {
            messagePending = true;
            return;
        }

        client.player.sendSystemMessage(Component.literal(
                "[Cold Breath] Debug HUD disabled after a rendering error. Use /coldbreath help for debug commands."));
        messageShownForCurrentContext = true;
        messagePending = false;
    }
}
