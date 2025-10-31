package com.sfdesat.coldbreath.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sfdesat.coldbreath.debug.DebugManager.CategoryDescriptor;
import com.sfdesat.coldbreath.debug.DebugManager.DebugCategory;
import com.sfdesat.coldbreath.debug.DebugManager.DebugLine;
import com.sfdesat.coldbreath.debug.DebugManager.DebugSnapshot;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import com.sfdesat.config.ConfigManager;

import java.util.Locale;
import java.util.Optional;

public final class DebugChat {

    private final DebugManager manager;

    public DebugChat(DebugManager manager) {
        this.manager = manager;
    }

    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("coldbreath")
                        .then(ClientCommandManager.literal("help")
                                .executes(this::sendHelp))
                        .then(ClientCommandManager.literal("print")
                                .executes(ctx -> printAll(ctx.getSource()))
                                .then(ClientCommandManager.literal("all")
                                        .executes(ctx -> printAll(ctx.getSource())))
                                .then(ClientCommandManager.argument("category", StringArgumentType.greedyString())
                                        .executes(this::printCategory))
                        )
        ));
    }

    private int sendHelp(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        if (!ensureCommandsEnabled(source)) return 0;
        source.sendFeedback(Text.literal("/coldbreath help - Show this help"));
        source.sendFeedback(Text.literal("/coldbreath print all - Print all debug information"));
        source.sendFeedback(Text.literal("/coldbreath print <category> - Print a single debug category"));

        source.sendFeedback(Text.literal("Available categories:"));
        for (CategoryDescriptor descriptor : manager.categoryDescriptors()) {
            source.sendFeedback(Text.literal(" - " + descriptor.displayName() + " (" + descriptor.key() + ")"));
        }
        return 1;
    }

    private int printAll(FabricClientCommandSource source) {
        if (!ensureCommandsEnabled(source)) return 0;
        DebugSnapshot snapshot = manager.capture();
        if (snapshot.isEmpty()) {
            source.sendFeedback(Text.literal("[Cold Breath] No debug data is available right now."));
            return 1;
        }

        for (DebugCategory category : snapshot.categories()) {
            source.sendFeedback(Text.literal("--- " + category.descriptor().displayName() + " ---"));
            for (DebugLine line : category.lines()) {
                source.sendFeedback(toColoredText(line));
            }
        }
        return 1;
    }

    private int printCategory(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        if (!ensureCommandsEnabled(source)) return 0;
        String raw = StringArgumentType.getString(ctx, "category");
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || "all".equals(normalized)) {
            return printAll(source);
        }

        Optional<CategoryDescriptor> descriptorOpt = manager.findDescriptor(normalized);
        if (descriptorOpt.isEmpty()) {
            source.sendError(Text.literal("Unknown Cold Breath debug category: " + raw));
            return 0;
        }

        DebugSnapshot snapshot = manager.capture();
        CategoryDescriptor descriptor = descriptorOpt.get();
        Optional<DebugCategory> categoryOpt = snapshot.getCategory(descriptor.key());
        if (categoryOpt.isEmpty()) {
            source.sendFeedback(Text.literal("[Cold Breath] Debug category '" + descriptor.displayName() + "' is not available right now."));
            return 1;
        }

        source.sendFeedback(Text.literal("--- " + descriptor.displayName() + " ---"));
        for (DebugLine line : categoryOpt.get().lines()) {
            source.sendFeedback(toColoredText(line));
        }
        return 1;
    }

    private Text toColoredText(DebugLine line) {
        MutableText text = Text.literal(line.text());
        int rgb = line.color() & 0xFFFFFF;
        text.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
        return text;
    }

    private boolean ensureCommandsEnabled(FabricClientCommandSource source) {
        if (ConfigManager.get().debugCommandsEnabled) return true;
        source.sendError(Text.literal("Cold Breath debug commands are disabled in the config."));
        return false;
    }
}

