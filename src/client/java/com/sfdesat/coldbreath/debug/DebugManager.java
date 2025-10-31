package com.sfdesat.coldbreath.debug;

import com.sfdesat.coldbreath.breath.BreathController;
import com.sfdesat.coldbreath.breath.EnvModel;
import com.sfdesat.coldbreath.breath.StateBlends;
import com.sfdesat.coldbreath.breath.VersionChecker;
import com.sfdesat.coldbreath.season.SeasonDetector;
import com.sfdesat.coldbreath.season.SeasonManager;
import com.sfdesat.coldbreath.season.SeasonPhase;
import com.sfdesat.config.ColdBreathConfig;
import com.sfdesat.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DebugManager {

    private static final List<CategoryDescriptor> CATEGORY_DESCRIPTORS = List.of(
            new CategoryDescriptor("breathing", "Breathing", List.of("breath")),
            new CategoryDescriptor("interval", "Breathing Interval", List.of()),
            new CategoryDescriptor("interval_range", "Interval Range", List.of("range", "minmax")),
            new CategoryDescriptor("temperature", "Temperature", List.of("temp")),
            new CategoryDescriptor("status", "Status", List.of()),
            new CategoryDescriptor("morning", "Morning Breath", List.of()),
            new CategoryDescriptor("time_range", "Daytime", List.of("time", "daytime")),
            new CategoryDescriptor("dimension", "Dimension", List.of("dim")),
            new CategoryDescriptor("season", "Season Phase", List.of("phase")),
            new CategoryDescriptor("season_mod", "Season Mod", List.of("seasonmod", "mod")),
            new CategoryDescriptor("version", "Version Checker", List.of("version", "ver"))
    );

    private static final Map<String, CategoryDescriptor> DESCRIPTORS_BY_KEY = buildDescriptorIndex();

    private final StateBlends blends;

    public DebugManager(StateBlends blends) {
        this.blends = Objects.requireNonNull(blends, "blends");
    }

    public List<CategoryDescriptor> categoryDescriptors() {
        return CATEGORY_DESCRIPTORS;
    }

    public Optional<CategoryDescriptor> findDescriptor(String aliasOrKey) {
        if (aliasOrKey == null || aliasOrKey.isBlank()) return Optional.empty();
        String normalized = aliasOrKey.trim().toLowerCase(Locale.ROOT);
        CategoryDescriptor byKey = DESCRIPTORS_BY_KEY.get(normalized);
        if (byKey != null) return Optional.of(byKey);
        for (CategoryDescriptor descriptor : CATEGORY_DESCRIPTORS) {
            if (descriptor.matches(normalized)) return Optional.of(descriptor);
        }
        return Optional.empty();
    }

    public DebugSnapshot capture() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DebugSnapshot.empty();

        ColdBreathConfig cfg = ConfigManager.get();
        Builder builder = new Builder();

        ClientWorld world = client.world;

        // Breathing state
        String breathingText = "breathing: unknown";
        int breathingColor = 0xFFFFFFFF;
        if (world != null && client.player != null) {
            boolean underwater = client.player.isSubmergedInWater();
            if (underwater && cfg.underwaterEnabled) {
                breathingText = "breathing: bubbles";
                breathingColor = 0xFF4EA3FF;
            } else {
                boolean eligible = EnvModel.isEligibleNow(world, client.player, cfg);
                breathingText = eligible ? "breathing: true" : "breathing: false";
                breathingColor = eligible ? 0xFF00FF00 : 0xFFFF0000;
            }
        }
        builder.addLine(descriptorFor("breathing"), new DebugLine(breathingText, breathingColor));

        // Interval information
        double baseInterval = BreathController.INSTANCE.getCurrentBaseIntervalSeconds(cfg);
        if (world != null && client.player != null && client.player.isSubmergedInWater() && cfg.underwaterEnabled) {
            baseInterval = Math.max(0.1, cfg.underwaterBaseIntervalSeconds);
        }
        builder.addLine(descriptorFor("interval"), new DebugLine(String.format(Locale.ROOT, "interval: %.1fs", baseInterval), 0xFFFFFFFF));

        double minInt;
        double maxInt;
        if (world != null && client.player != null && client.player.isSubmergedInWater() && cfg.underwaterEnabled) {
            double base = Math.max(0.1, cfg.underwaterBaseIntervalSeconds);
            double dev = Math.max(0.0, cfg.underwaterIntervalDeviationSeconds);
            minInt = Math.max(0.1, base - dev);
            maxInt = Math.max(minInt, base + dev);
        } else {
            double[] range = BreathController.INSTANCE.getCurrentIntervalMinMaxSeconds(cfg);
            minInt = range[0];
            maxInt = range[1];
        }
        builder.addLine(descriptorFor("interval_range"), new DebugLine(String.format(Locale.ROOT, "min/max: %.1fs / %.1fs", minInt, maxInt), 0xFFFFFFFF));

        // Temperature information only when in world
        if (world != null && client.player != null) {
            BlockPos pos = client.player.getBlockPos();
            float baseTemp = world.getBiome(pos).value().getTemperature();
            float effTemp = EnvModel.computeEffectiveTemperature(world, pos, cfg);
            int sea = world.getSeaLevel();
            int alt = pos.getY() - sea;
            double seasonModifier = SeasonManager.getTemperatureOffset();
            String tempLine = String.format(Locale.ROOT, "temp: %.3f (base: %.3f), alt: %+d, season: %+.3f", effTemp, baseTemp, alt, seasonModifier);
            builder.addLine(descriptorFor("temperature"), new DebugLine(tempLine, 0xFFFFFFFF));
        }

        // Status information
        builder.addLine(descriptorFor("status"), new DebugLine(String.format(Locale.ROOT, "status: %s | sprint: %.2f | health: %.2f", getDebugState(cfg), blends.getSprintBlend(), blends.getHealthBlend()), 0xFFFFFFFF));

        // Morning breath and time information
        if (world != null && client.player != null) {
            long dayTime = world.getTimeOfDay() % 24000L;
            boolean inWindow = EnvModel.isWithinDayWindow(dayTime, cfg.morningBreathStartTick, cfg.morningBreathEndTick);
            float temp = EnvModel.computeEffectiveTemperature(world, client.player.getBlockPos(), cfg);
            boolean okTemp = temp > cfg.alwaysBreathTemperature && temp <= cfg.maxMorningBreathTemperature;
            boolean seasonMorningEnabled = SeasonManager.isMorningBreathEnabled(cfg.morningBreathEnabled);
            boolean morningActive = seasonMorningEnabled && inWindow && okTemp;
            int morningColor = morningActive ? 0xFF00FF00 : 0xFFFF0000;
            String morningLabel = seasonMorningEnabled ? (morningActive ? "true" : "false") : "disabled";
            builder.addLine(descriptorFor("morning"), new DebugLine("morning breath: " + morningLabel, morningColor));

            String range = String.format(Locale.ROOT, "time: %d | morning range: %d-%d", dayTime, cfg.morningBreathStartTick, cfg.morningBreathEndTick);
            builder.addLine(descriptorFor("time_range"), new DebugLine(range, 0xFFFFFFFF));
        }

        // Dimension information
        String dimText = "dim: unknown";
        if (world != null) {
            EnvModel.DimensionKind kind = EnvModel.getDimensionKind(world);
            dimText = "dim: " + kind.name().toLowerCase(Locale.ROOT) + " (" + world.getRegistryKey().getValue() + ")";
        }
        builder.addLine(descriptorFor("dimension"), new DebugLine(dimText, 0xFFFFFFFF));

        // Season information
        SeasonPhase phase = SeasonManager.getCurrentPhase();
        String phaseDisplay = phase == SeasonPhase.UNKNOWN ? "none" : phase.displayName();
        builder.addLine(descriptorFor("season"), new DebugLine("season: " + phaseDisplay, 0xFFFFFFFF));

        String seasonModDisplay = SeasonManager.getCurrentMod() == SeasonDetector.SeasonMod.VANILLA ? "none" : SeasonDetector.getDisplayName();
        builder.addLine(descriptorFor("season_mod"), new DebugLine("season mod: " + seasonModDisplay, 0xFFFFFFFF));

        // Version checker details
        builder.addLine(descriptorFor("version"), new DebugLine("version: " + VersionChecker.getLastUsedConstructor(), 0xFFFFFFFF));

        return builder.isEmpty() ? DebugSnapshot.empty() : builder.build();
    }

    private String getDebugState(ColdBreathConfig cfg) {
        if (!cfg.sprintingIntervalsEnabled && !cfg.healthBasedBreathingEnabled) return "normal";
        double baseNormal = Math.max(0.1, cfg.baseIntervalSeconds);
        double baseSprint = Math.max(0.1, cfg.sprintBaseIntervalSeconds);
        double baseHealth = Math.max(0.1, cfg.lowHealthIntervalSeconds);
        double afterSprint = lerp(baseNormal, baseSprint, cfg.sprintingIntervalsEnabled ? blends.getSprintBlend() : 0.0);
        double afterHealth = lerp(baseNormal, baseHealth, cfg.healthBasedBreathingEnabled ? blends.getHealthBlend() : 0.0);
        boolean healthCtrl = cfg.healthBasedBreathingEnabled && afterHealth <= afterSprint;
        boolean sprintCtrl = cfg.sprintingIntervalsEnabled && afterSprint < afterHealth;
        if (healthCtrl) {
            if (blends.getHealthBlend() >= 0.7) return "critical health";
            if (blends.getHealthBlend() >= 0.3) return "low health";
            return "health priority";
        }
        if (sprintCtrl) {
            if (blends.getSprintBlend() >= 0.95) return "sprinting";
            if (blends.getSprintBlend() <= 0.05) return "normal";
            if (blends.getSprintBlend() > blends.getPrevSprintBlend() + 1e-6) return "building up";
            if (blends.getSprintBlend() < blends.getPrevSprintBlend() - 1e-6) return "building down";
            return "transitional";
        }
        return "normal";
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static CategoryDescriptor descriptorFor(String key) {
        CategoryDescriptor descriptor = DESCRIPTORS_BY_KEY.get(key);
        if (descriptor == null) throw new IllegalArgumentException("Unknown debug category: " + key);
        return descriptor;
    }

    private static Map<String, CategoryDescriptor> buildDescriptorIndex() {
        Map<String, CategoryDescriptor> index = new LinkedHashMap<>();
        for (CategoryDescriptor descriptor : CATEGORY_DESCRIPTORS) {
            index.put(descriptor.key(), descriptor);
        }
        return Collections.unmodifiableMap(index);
    }

    public static final class DebugSnapshot {
        private static final DebugSnapshot EMPTY = new DebugSnapshot(List.of());

        private final List<DebugCategory> categories;
        private final Map<String, DebugCategory> categoriesByKey;
        private final List<DebugLine> flattenedLines;

        private DebugSnapshot(List<DebugCategory> categories) {
            this.categories = List.copyOf(categories);
            Map<String, DebugCategory> byKey = new LinkedHashMap<>();
            List<DebugLine> lines = new ArrayList<>();
            for (DebugCategory category : this.categories) {
                byKey.put(category.descriptor().key(), category);
                lines.addAll(category.lines());
            }
            this.categoriesByKey = Collections.unmodifiableMap(byKey);
            this.flattenedLines = Collections.unmodifiableList(lines);
        }

        public static DebugSnapshot empty() {
            return EMPTY;
        }

        public boolean isEmpty() {
            return flattenedLines.isEmpty();
        }

        public int totalLineCount() {
            return flattenedLines.size();
        }

        public List<DebugLine> lines() {
            return flattenedLines;
        }

        public List<DebugCategory> categories() {
            return categories;
        }

        public Optional<DebugCategory> getCategory(String key) {
            return Optional.ofNullable(categoriesByKey.get(key));
        }
    }

    public record DebugCategory(CategoryDescriptor descriptor, List<DebugLine> lines) {
        public DebugCategory {
            lines = List.copyOf(lines);
        }
    }

    public record DebugLine(String text, int color) {
    }

    public record CategoryDescriptor(String key, String displayName, List<String> aliases) {
        public CategoryDescriptor {
            key = Objects.requireNonNull(key, "key").toLowerCase(Locale.ROOT);
            displayName = Objects.requireNonNull(displayName, "displayName");
            List<String> normalized = new ArrayList<>();
            if (aliases != null) {
                for (String alias : aliases) {
                    if (alias == null || alias.isBlank()) continue;
                    normalized.add(alias.toLowerCase(Locale.ROOT));
                }
            }
            aliases = Collections.unmodifiableList(normalized);
        }

        private boolean matches(String alias) {
            return aliases.contains(alias);
        }
    }

    private static final class Builder {
        private final LinkedHashMap<String, CategoryAccumulator> lines = new LinkedHashMap<>();

        void addLine(CategoryDescriptor descriptor, DebugLine line) {
            CategoryAccumulator accumulator = lines.computeIfAbsent(descriptor.key(), key -> new CategoryAccumulator(descriptor));
            accumulator.lines.add(line);
        }

        boolean isEmpty() {
            return lines.isEmpty();
        }

        DebugSnapshot build() {
            List<DebugCategory> categories = new ArrayList<>();
            for (CategoryAccumulator accumulator : lines.values()) {
                categories.add(new DebugCategory(accumulator.descriptor, List.copyOf(accumulator.lines)));
            }
            return new DebugSnapshot(categories);
        }
    }

    private static final class CategoryAccumulator {
        private final CategoryDescriptor descriptor;
        private final List<DebugLine> lines = new ArrayList<>();

        private CategoryAccumulator(CategoryDescriptor descriptor) {
            this.descriptor = descriptor;
        }
    }
}

