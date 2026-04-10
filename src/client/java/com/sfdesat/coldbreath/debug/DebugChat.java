package com.sfdesat.coldbreath.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sfdesat.coldbreath.debug.DebugManager.CategoryDescriptor;
import com.sfdesat.coldbreath.debug.DebugManager.DebugCategory;
import com.sfdesat.coldbreath.debug.DebugManager.DebugLine;
import com.sfdesat.coldbreath.debug.DebugManager.DebugSnapshot;
import com.sfdesat.config.ConfigManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;
import java.util.Optional;

public final class DebugChat {

    private final DebugManager manager;

    public DebugChat(DebugManager manager) {
        this.manager = manager;
    }

    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("coldbreath")
                        .then(ClientCommands.literal("help")
                                .executes(this::sendHelp))
                        .then(ClientCommands.literal("print")
                                .executes(ctx -> printAll(ctx.getSource()))
                                .then(ClientCommands.literal("all")
                                        .executes(ctx -> printAll(ctx.getSource())))
                                .then(ClientCommands.argument("category", StringArgumentType.greedyString())
                                        .executes(this::printCategory))
                        )
        ));
    }

    private int sendHelp(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        if (!ensureCommandsEnabled(source)) return 0;
        source.sendFeedback(Component.literal("/coldbreath help - Show this help"));
        source.sendFeedback(Component.literal("/coldbreath print all - Print all debug information"));
        source.sendFeedback(Component.literal("/coldbreath print <category> - Print a single debug category"));

        source.sendFeedback(Component.literal("Available categories:"));
        for (CategoryDescriptor descriptor : manager.categoryDescriptors()) {
            source.sendFeedback(Component.literal(" - " + descriptor.displayName() + " (" + descriptor.key() + ")"));
        }
        return 1;
    }

    private int printAll(FabricClientCommandSource source) {
        if (!ensureCommandsEnabled(source)) return 0;
        DebugSnapshot snapshot = manager.capture();
        if (snapshot.isEmpty()) {
            source.sendFeedback(Component.literal("[Cold Breath] No debug data is available right now."));
            return 1;
        }

        for (DebugCategory category : snapshot.categories()) {
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
            source.sendError(Component.literal("Unknown Cold Breath debug category: " + raw));
            return 0;
        }

        DebugSnapshot snapshot = manager.capture();
        CategoryDescriptor descriptor = descriptorOpt.get();
        Optional<DebugCategory> categoryOpt = snapshot.getCategory(descriptor.key());
        if (categoryOpt.isEmpty()) {
            source.sendFeedback(Component.literal("[Cold Breath] Debug category '" + descriptor.displayName() + "' is not available right now."));
            return 1;
        }

        for (DebugLine line : categoryOpt.get().lines()) {
            source.sendFeedback(toColoredText(line));
        }
        return 1;
    }

    private Component toColoredText(DebugLine line) {
        MutableComponent text = Component.literal(line.text());
        int rgb = line.color() & 0xFFFFFF;
        text.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
        return text;
    }

    private boolean ensureCommandsEnabled(FabricClientCommandSource source) {
        if (ConfigManager.get().debugCommandsEnabled) return true;
        source.sendError(Component.literal("Cold Breath debug commands are disabled in the config."));
        return false;
    }
}
