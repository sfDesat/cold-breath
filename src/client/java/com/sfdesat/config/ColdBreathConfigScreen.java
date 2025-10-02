package com.sfdesat.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ColdBreathConfigScreen {
    private ColdBreathConfigScreen() {}

    public static Screen create(Screen parent) {
        final ColdBreathConfig cfg = ConfigManager.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Cold Breath Config"))
                .setSavingRunnable(ConfigManager::save);

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("Cold Breath"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        // --- Top-level toggles & base timing ---
        var enabledEntry = eb.startBooleanToggle(Text.literal("Enabled"), cfg.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Master switch for Cold Breath."))
                .setSaveConsumer(v -> cfg.enabled = v)
                .build();

        var onlyColdEntry = eb.startBooleanToggle(Text.literal("Only in Cold Temperatures"), cfg.onlyInColdBiomes)
                .setDefaultValue(true)
                .setTooltip(
                        Text.literal("If enabled, breaths only appear where temperature <= 0.15f."),
                        Text.literal("This includes high altitudes and cold biomes. Nether/End visibility is controlled below.")
                )
                .setSaveConsumer(v -> cfg.onlyInColdBiomes = v)
                .build();

        var altitudeToggleEntry = eb.startBooleanToggle(Text.literal("Enable Altitude Adjustment"), cfg.altitudeAdjustmentEnabled)
                .setDefaultValue(true)
                .setTooltip(
                        Text.literal("If enabled, temperature decreases with altitude above sea level."),
                        Text.literal("This allows cold breath to appear at high altitudes in warmer biomes.")
                )
                .setSaveConsumer(v -> cfg.altitudeAdjustmentEnabled = v)
                .build();

        var altitudeRateEntry = eb.startIntSlider(
                        Text.literal("Altitude Temperature Rate"),
                        (int) Math.round(cfg.altitudeTemperatureRate * 100000), // Convert to int (0-1000 range)
                        0, 1000 // 0.00000–0.01000 per block
                )
                .setDefaultValue(125) // 0.00125
                .setTextGetter(i -> Text.literal(String.format("%.5f", i / 100000.0)))
                .setTooltip(
                        Text.literal("Temperature decrease per block above sea level (0.00000–0.01000)."),
                        Text.literal("Default 0.00125 matches Minecraft's vanilla behavior.")
                )
                .setSaveConsumer(i -> cfg.altitudeTemperatureRate = i / 100000.0)
                .build();

        var baseIntervalEntry = eb.startIntSlider(
                        Text.literal("Base Interval (seconds)"),
                        (int) Math.round(cfg.baseIntervalSeconds * 10),
                        20, 100 // 2.0–10.0 s
                )
                .setDefaultValue(50) // 5.0 s
                .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Text.literal("Average delay between breaths while not sprinting (2.0–10.0 s)."))
                .setSaveConsumer(i -> cfg.baseIntervalSeconds = i / 10.0)
                .build();

        var baseDevEntry = eb.startIntSlider(
                        Text.literal("Deviation (seconds)"),
                        (int) Math.round(cfg.intervalDeviationSeconds * 10),
                        0, 20 // 0.0–2.0 s
                )
                .setDefaultValue(10) // 1.0 s
                .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Text.literal("Random variation added/subtracted from base interval (0.0–2.0 s)."))
                .setSaveConsumer(i -> cfg.intervalDeviationSeconds = i / 10.0)
                .build();

        general.addEntry(enabledEntry);
        general.addEntry(onlyColdEntry);
        general.addEntry(altitudeToggleEntry);
        general.addEntry(altitudeRateEntry);
        general.addEntry(baseIntervalEntry);
        general.addEntry(baseDevEntry);

        // --- Sprinting subcategory ---
        var sprintToggleEntry = eb.startBooleanToggle(Text.literal("Enable Sprinting-specific Intervals"), cfg.sprintingIntervalsEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Use separate timing while sprinting, with smooth ramp in/out."))
                .setSaveConsumer(v -> cfg.sprintingIntervalsEnabled = v)
                .build();

        var sprintBaseEntry = eb.startIntSlider(
                        Text.literal("Sprinting Base Interval (seconds)"),
                        (int) Math.round(cfg.sprintBaseIntervalSeconds * 10),
                        10, 80 // 1.0–8.0 s
                )
                .setDefaultValue(30) // 3.0 s
                .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Text.literal("Average delay between breaths while sprinting (1.0–8.0 s)."))
                .setSaveConsumer(i -> cfg.sprintBaseIntervalSeconds = i / 10.0)
                .build();

        var sprintDevEntry = eb.startIntSlider(
                        Text.literal("Sprinting Deviation (seconds)"),
                        (int) Math.round(cfg.sprintIntervalDeviationSeconds * 10),
                        0, 10 // 0.0–1.0 s
                )
                .setDefaultValue(5) // 0.5 s
                .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Text.literal("Random variation while sprinting (0.0–1.0 s)."))
                .setSaveConsumer(i -> cfg.sprintIntervalDeviationSeconds = i / 10.0)
                .build();

        var sprintUpEntry = eb.startIntSlider(
                        Text.literal("Sprint Build-up (seconds)"),
                        (int) Math.round(cfg.sprintBuildUpSeconds * 10),
                        0, 300 // 0.0–30.0 s
                )
                .setDefaultValue(80) // 8.0 s
                .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Text.literal("Time to ramp from normal to sprint cadence (0.0–30.0 s)."))
                .setSaveConsumer(i -> cfg.sprintBuildUpSeconds = i / 10.0)
                .build();

        var sprintDownEntry = eb.startIntSlider(
                        Text.literal("Sprint Build-down (seconds)"),
                        (int) Math.round(cfg.sprintBuildDownSeconds * 10),
                        0, 300 // 0.0–30.0 s
                )
                .setDefaultValue(120) // 12.0 s
                .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Text.literal("Time to ramp from sprint back to normal (0.0–30.0 s)."))
                .setSaveConsumer(i -> cfg.sprintBuildDownSeconds = i / 10.0)
                .build();

        @SuppressWarnings({"rawtypes"})
        List<AbstractConfigListEntry> sprintEntries = new ArrayList<>();
        sprintEntries.add(sprintToggleEntry);
        sprintEntries.add(sprintBaseEntry);
        sprintEntries.add(sprintDevEntry);
        sprintEntries.add(sprintUpEntry);
        sprintEntries.add(sprintDownEntry);

        AbstractConfigListEntry<?> sprintSub =
                eb.startSubCategory(Text.literal("Sprinting"), sprintEntries).build();

        // --- Visuals subcategory ---
        var forwardEntry = eb.startDoubleField(Text.literal("Forward Offset"), cfg.forwardOffset)
                .setDefaultValue(0.3)
                .setMin(0.0)
                .setTooltip(Text.literal("Meters in front of the player head to spawn particles."))
                .setSaveConsumer(v -> cfg.forwardOffset = v)
                .build();

        var downEntry = eb.startDoubleField(Text.literal("Down Offset"), cfg.downOffset)
                .setDefaultValue(0.2)
                .setMin(0.0)
                .setTooltip(Text.literal("Meters below the player head to spawn particles."))
                .setSaveConsumer(v -> cfg.downOffset = v)
                .build();

        var burstEntry = eb.startIntField(Text.literal("Burst Duration (ticks)"), cfg.breathBurstDurationTicks)
                .setDefaultValue(10)
                .setMin(1)
                .setTooltip(Text.literal("How long a breath burst continues spawning particles (ticks)."))
                .setSaveConsumer(v -> cfg.breathBurstDurationTicks = v)
                .build();

        String currentHex = String.format("#%06X", (0xFFFFFF & cfg.breathColor));
        var colorEntry = eb.startStrField(Text.literal("Breath Color (hex)"), currentHex)
                .setDefaultValue("#E6F2FF")
                .setTooltip(Text.literal("RGB hex for breath particles. Accepts #RRGGBB, 0xRRGGBB, or RRGGBB."))
                .setSaveConsumer(str -> {
                    if (str == null) return;
                    String s = str.trim();
                    if (s.startsWith("#")) s = s.substring(1);
                    if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
                    if (s.length() == 6) {
                        try {
                            int rgb = Integer.parseInt(s, 16) & 0xFFFFFF;
                            cfg.breathColor = rgb;
                        } catch (NumberFormatException ignored) {}
                    }
                })
                .build();

        var sizeEntry = eb.startDoubleField(Text.literal("Breath Size"), cfg.breathSize)
                .setDefaultValue(0.6)
                .setMin(0.1)
                .setTooltip(Text.literal("Particle scale for breath particles (min 0.1)."))
                .setSaveConsumer(v -> cfg.breathSize = v)
                .build();

        @SuppressWarnings({"rawtypes"})
        List<AbstractConfigListEntry> visualEntries = new ArrayList<>();
        visualEntries.add(forwardEntry);
        visualEntries.add(downEntry);
        visualEntries.add(burstEntry);
        visualEntries.add(colorEntry);
        visualEntries.add(sizeEntry);

        AbstractConfigListEntry<?> visualsSub =
                eb.startSubCategory(Text.literal("Visuals"), visualEntries).build();

        // --- Underwater subcategory ---
        var uwToggle = eb.startBooleanToggle(Text.literal("Enable Underwater Breaths"), cfg.underwaterEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show bubble breaths when underwater (has air bar)."))
                .setSaveConsumer(v -> cfg.underwaterEnabled = v)
                .build();

        var uwBase = eb.startIntSlider(
                        Text.literal("Underwater Base Interval (seconds)"),
                        (int) Math.round(cfg.underwaterBaseIntervalSeconds * 10),
                        40, 200 // 4.0–20.0 s
                )
                .setDefaultValue(80) // 8.0 s
                .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Text.literal("Average delay between bubble breaths underwater (4.0–20.0 s)."))
                .setSaveConsumer(i -> cfg.underwaterBaseIntervalSeconds = i / 10.0)
                .build();

        var uwDev = eb.startIntSlider(
                        Text.literal("Underwater Deviation (seconds)"),
                        (int) Math.round(cfg.underwaterIntervalDeviationSeconds * 10),
                        0, 40 // 0.0–4.0 s
                )
                .setDefaultValue(10) // 1.0 s
                .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Text.literal("Random variation for underwater breaths (0.0–4.0 s)."))
                .setSaveConsumer(i -> cfg.underwaterIntervalDeviationSeconds = i / 10.0)
                .build();

        @SuppressWarnings({"rawtypes"})
        List<AbstractConfigListEntry> uwEntries = new ArrayList<>();
        uwEntries.add(uwToggle);
        uwEntries.add(uwBase);
        uwEntries.add(uwDev);

        AbstractConfigListEntry<?> underwaterSub =
                eb.startSubCategory(Text.literal("Underwater"), uwEntries).build();

        // --- Visibility subcategory ---
        var visibleCreativeEntry = eb.startBooleanToggle(Text.literal("Visible in Creative"), cfg.visibleInCreative)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Show breaths while in Creative mode."))
                .setSaveConsumer(v -> cfg.visibleInCreative = v)
                .build();

        var visibleNetherEntry = eb.startBooleanToggle(Text.literal("Visible in Nether"), cfg.visibleInNether)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Show breaths while in the Nether."))
                .setSaveConsumer(v -> cfg.visibleInNether = v)
                .build();

        var visibleEndEntry = eb.startBooleanToggle(Text.literal("Visible in End"), cfg.visibleInEnd)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show breaths while in the End."))
                .setSaveConsumer(v -> cfg.visibleInEnd = v)
                .build();

        @SuppressWarnings({"rawtypes"})
        List<AbstractConfigListEntry> dimensionsEntries = new ArrayList<>();
        dimensionsEntries.add(visibleCreativeEntry);
        dimensionsEntries.add(visibleNetherEntry);
        dimensionsEntries.add(visibleEndEntry);

        AbstractConfigListEntry<?> dimensionsSub =
                eb.startSubCategory(Text.literal("Visibility"), dimensionsEntries).build();

        // --- Debug (bottom) ---
        var debugEntry = eb.startBooleanToggle(Text.literal("Debug Overlay"), cfg.debugEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Show on-screen state and sprint blend values."))
                .setSaveConsumer(v -> cfg.debugEnabled = v)
                .build();

        // Attach subcategories in order
        general.addEntry(sprintSub);
        general.addEntry(underwaterSub);
        general.addEntry(visualsSub);
        general.addEntry(dimensionsSub);
        general.addEntry(debugEntry);

        return builder.build();
    }
}
