package com.sfdesat.config;

import com.sfdesat.coldbreath.season.SeasonManager;
import com.sfdesat.coldbreath.season.SeasonPhase;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ColdBreathConfigScreen {
    private ColdBreathConfigScreen() {}

    public static Screen create(Screen parent) {
        final ColdBreathConfig cfg = ConfigManager.get();
		cfg.normalizeSeasonConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Cold Breath Config"))
				.setSavingRunnable(() -> {
					cfg.normalizeSeasonConfig();
					SeasonManager.applyConfig(cfg);
					ConfigManager.save();
				});

        ConfigCategory mainCat = builder.getOrCreateCategory(Component.literal("Main"));
		ConfigCategory breathingCat = builder.getOrCreateCategory(Component.literal("Breathing"));
		ConfigCategory seasonsCat = builder.getOrCreateCategory(Component.literal("Seasons"));
		ConfigCategory visualsCat = builder.getOrCreateCategory(Component.literal("Visuals"));
		ConfigCategory visibilityCat = builder.getOrCreateCategory(Component.literal("Visibility"));
		ConfigCategory debugCat = builder.getOrCreateCategory(Component.literal("Debug"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        // --- Main ---
		var enabledEntry = eb.startBooleanToggle(Component.literal("Enabled"), cfg.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Master switch for Cold Breath."))
                .setSaveConsumer(v -> cfg.enabled = v)
                .build();

		var alwaysShowEntry = eb.startBooleanToggle(Component.literal("Always Show Breath"), cfg.alwaysShowBreath)
				.setDefaultValue(false)
				.setTooltip(
						Component.literal("If enabled, breath appears even in warmer temperatures."),
						Component.literal("When off, breath follows the Breath Temperature threshold and condensation window."),
						Component.literal("Nether/End visibility is configured in the Visibility tab.")
				)
				.setSaveConsumer(v -> cfg.alwaysShowBreath = v)
				.build();

		var altitudeToggleEntry = eb.startBooleanToggle(Component.literal("Enable Altitude Effects"), cfg.altitudeAdjustmentEnabled)
                .setDefaultValue(true)
                .setTooltip(
						Component.literal("If enabled, temperature decreases with altitude above sea level."),
						Component.literal("This allows breath to show at high altitudes in warmer biomes.")
                )
                .setSaveConsumer(v -> cfg.altitudeAdjustmentEnabled = v)
                .build();

		var altitudeRateEntry = eb.startIntSlider(
					Component.literal("Altitude Rate"),
                        (int) Math.round(cfg.altitudeTemperatureRate * 100000), // Convert to int (0-1000 range)
                        0, 1000 // 0.00000–0.01000 per block
                )
                .setDefaultValue(125) // 0.00125
                .setTextGetter(i -> Component.literal(String.format("%.5f", i / 100000.0)))
			.setTooltip(
					Component.literal("Temperature decrease per block above sea level (0.00000–0.01000)."),
					Component.literal("Default 0.00125 matches Minecraft's vanilla behavior.")
			)
                .setSaveConsumer(i -> cfg.altitudeTemperatureRate = i / 100000.0)
                .build();

		var alwaysBreathTempEntry = eb.startIntSlider(
					Component.literal("Breath Temperature"),
                        (int) Math.round(cfg.alwaysBreathTemperature * 1000), // Convert to int (0-1000 range)
                        0, 1000 // 0.000–1.000
                )
                .setDefaultValue(150) // 0.15
                .setTextGetter(i -> Component.literal(String.format("%.3f", i / 1000.0)))
			.setTooltip(
					Component.literal("Temperature threshold where breath always appears (0.000–1.000)."),
					Component.literal("Default 0.15 - breath always shows in biomes colder than this.")
			)
                .setSaveConsumer(i -> cfg.alwaysBreathTemperature = i / 1000.0)
                .build();


        var baseIntervalEntry = eb.startIntSlider(
                        Component.literal("Base Interval (seconds)"),
                        (int) Math.round(cfg.baseIntervalSeconds * 10),
                        20, 100 // 2.0–10.0 s
                )
                .setDefaultValue(50) // 5.0 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Component.literal("Average delay between breaths while not sprinting (2.0–10.0 s)."))
                .setSaveConsumer(i -> cfg.baseIntervalSeconds = i / 10.0)
                .build();

        var baseDevEntry = eb.startIntSlider(
                        Component.literal("Deviation (seconds)"),
                        (int) Math.round(cfg.intervalDeviationSeconds * 10),
                        0, 20 // 0.0–2.0 s
                )
                .setDefaultValue(10) // 1.0 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Component.literal("Random variation added/subtracted from base interval (0.0–2.0 s)."))
                .setSaveConsumer(i -> cfg.intervalDeviationSeconds = i / 10.0)
                .build();

		mainCat.addEntry(enabledEntry);
		mainCat.addEntry(alwaysShowEntry);
        mainCat.addEntry(altitudeToggleEntry);
        mainCat.addEntry(altitudeRateEntry);
        mainCat.addEntry(alwaysBreathTempEntry);
        mainCat.addEntry(baseIntervalEntry);
        mainCat.addEntry(baseDevEntry);

        // --- Breathing (sprinting) ---
		var sprintToggleEntry = eb.startBooleanToggle(Component.literal("Enable Sprinting Interval"), cfg.sprintingIntervalsEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Use separate timing while sprinting, with smooth ramp in/out."))
                .setSaveConsumer(v -> cfg.sprintingIntervalsEnabled = v)
                .build();

        var sprintBaseEntry = eb.startIntSlider(
                        Component.literal("Sprinting Base Interval (seconds)"),
                        (int) Math.round(cfg.sprintBaseIntervalSeconds * 10),
                        10, 80 // 1.0–8.0 s
                )
                .setDefaultValue(30) // 3.0 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Component.literal("Average delay between breaths while sprinting (1.0–8.0 s)."))
                .setSaveConsumer(i -> cfg.sprintBaseIntervalSeconds = i / 10.0)
                .build();

        var sprintDevEntry = eb.startIntSlider(
                        Component.literal("Sprinting Deviation (seconds)"),
                        (int) Math.round(cfg.sprintIntervalDeviationSeconds * 10),
                        0, 10 // 0.0–1.0 s
                )
                .setDefaultValue(5) // 0.5 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Component.literal("Random variation while sprinting (0.0–1.0 s)."))
                .setSaveConsumer(i -> cfg.sprintIntervalDeviationSeconds = i / 10.0)
                .build();

        var sprintUpEntry = eb.startIntSlider(
                        Component.literal("Sprint Build-up (seconds)"),
                        (int) Math.round(cfg.sprintBuildUpSeconds * 10),
                        0, 300 // 0.0–30.0 s
                )
                .setDefaultValue(80) // 8.0 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Component.literal("Time to ramp from normal to sprint cadence (0.0–30.0 s)."))
                .setSaveConsumer(i -> cfg.sprintBuildUpSeconds = i / 10.0)
                .build();

        var sprintDownEntry = eb.startIntSlider(
                        Component.literal("Sprint Build-down (seconds)"),
                        (int) Math.round(cfg.sprintBuildDownSeconds * 10),
                        0, 300 // 0.0–30.0 s
                )
                .setDefaultValue(120) // 12.0 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Component.literal("Time to ramp from sprint back to normal (0.0–30.0 s)."))
                .setSaveConsumer(i -> cfg.sprintBuildDownSeconds = i / 10.0)
                .build();

        @SuppressWarnings({"rawtypes"})
        List<AbstractConfigListEntry> sprintEntries = new ArrayList<>();
        sprintEntries.add(sprintToggleEntry);
        sprintEntries.add(sprintBaseEntry);
        sprintEntries.add(sprintDevEntry);
        sprintEntries.add(sprintUpEntry);
        sprintEntries.add(sprintDownEntry);
        AbstractConfigListEntry<?> sprintSub = eb.startSubCategory(Component.literal("Sprinting"), sprintEntries).build();
        breathingCat.addEntry(sprintSub);

		// --- Breathing (breath condensation) ---
		var condensationToggleEntry = eb.startBooleanToggle(Component.literal("Enable Breath Condensation"), cfg.breathCondensationEnabled)
				.setDefaultValue(true)
				.setTooltip(
						Component.literal("Enable visible breath condensation during cool time windows."),
						Component.literal("Breath will appear in moderate temperatures during the configured condensation window.")
				)
				.setSaveConsumer(v -> cfg.breathCondensationEnabled = v)
				.build();

		var condensationStartEntry = eb.startLongField(Component.literal("Condensation Start (ticks)"), cfg.breathCondensationStartTick)
				.setDefaultValue(22500L)
				.setMin(0L)
				.setMax(23999L)
				.setTooltip(
						Component.literal("Start of the breath condensation window (0-23999 ticks)."),
						Component.literal("Default 22500 - condensation begins at this time.")
				)
				.setSaveConsumer(v -> cfg.breathCondensationStartTick = v)
				.build();

		var condensationEndEntry = eb.startLongField(Component.literal("Condensation End (ticks)"), cfg.breathCondensationEndTick)
				.setDefaultValue(1500L)
				.setMin(0L)
				.setMax(23999L)
				.setTooltip(
						Component.literal("End of the breath condensation window (0-23999 ticks)."),
						Component.literal("Default 1500 - condensation stops at this time.")
				)
				.setSaveConsumer(v -> cfg.breathCondensationEndTick = v)
				.build();

		var maxCondensationTempEntry = eb.startIntSlider(
					Component.literal("Max Condensation Temperature"),
					(int) Math.round(cfg.maxBreathCondensationTemperature * 1000), // Convert to int (0-1000 range)
					0, 1000 // 0.000–1.000
			)
			.setDefaultValue(700) // 0.7
			.setTextGetter(i -> Component.literal(String.format("%.3f", i / 1000.0)))
			.setTooltip(
					Component.literal("Maximum temperature for breath condensation (0.000–1.000)."),
					Component.literal("Default 0.7 - condensation appears between always-breath temp and this value.")
			)
			.setSaveConsumer(i -> cfg.maxBreathCondensationTemperature = i / 1000.0)
			.build();

		@SuppressWarnings({"rawtypes"})
		List<AbstractConfigListEntry> condensationEntries = new ArrayList<>();
		condensationEntries.add(condensationToggleEntry);
		condensationEntries.add(condensationStartEntry);
		condensationEntries.add(condensationEndEntry);
		condensationEntries.add(maxCondensationTempEntry);
		AbstractConfigListEntry<?> condensationSub = eb.startSubCategory(Component.literal("Breath Condensation"), condensationEntries).build();
		breathingCat.addEntry(condensationSub);

        // --- Breathing (health) ---
        var healthBreathingToggleEntry = eb.startBooleanToggle(Component.literal("Enable Health-based Breathing"), cfg.healthBasedBreathingEnabled)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Enable breathing intervals that change based on player health."),
                        Component.literal("Players will breathe faster when they have lower health.")
                )
                .setSaveConsumer(v -> cfg.healthBasedBreathingEnabled = v)
                .build();

        var lowHealthIntervalEntry = eb.startIntSlider(
                        Component.literal("Low Health Interval (seconds)"),
                        (int) Math.round(cfg.lowHealthIntervalSeconds * 10),
                        5, 20 // 0.5–2.0 s (wider to allow requested min/max)
                )
                .setDefaultValue(10) // 1.0 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(
                        Component.literal("Breathing interval when at 0 hearts (0.5–2.0 s)."),
                        Component.literal("Default 1.0 - fast breathing when critically injured.")
                )
                .setSaveConsumer(i -> cfg.lowHealthIntervalSeconds = i / 10.0)
                .build();

        var healthDeviationEntry = eb.startIntSlider(
                        Component.literal("Health Interval Deviation (seconds)"),
                        (int) Math.round(cfg.healthIntervalDeviationSeconds * 10),
                        0, 10 // 0.0–1.0 s (allows max of 2.0 around 1.0 if desired)
                )
                .setDefaultValue(2) // 0.5 s default
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(
                        Component.literal("Random variation for health breathing (0.0–1.0 s)."),
                        Component.literal("Default 0.5 - allows min ~0.5 s and max ~2.0 s around 1.0 s base.")
                )
                .setSaveConsumer(i -> cfg.healthIntervalDeviationSeconds = i / 10.0)
                .build();

        @SuppressWarnings({"rawtypes"})
        List<AbstractConfigListEntry> healthEntries = new ArrayList<>();
        healthEntries.add(healthBreathingToggleEntry);
        healthEntries.add(lowHealthIntervalEntry);
        healthEntries.add(healthDeviationEntry);
        AbstractConfigListEntry<?> healthSub = eb.startSubCategory(Component.literal("Health-based"), healthEntries).build();
        breathingCat.addEntry(healthSub);

        // --- Visuals ---
        var forwardEntry = eb.startDoubleField(Component.literal("Forward Offset"), cfg.forwardOffset)
                .setDefaultValue(0.3)
                .setMin(0.0)
                .setTooltip(Component.literal("Meters in front of the player head to spawn particles."))
                .setSaveConsumer(v -> cfg.forwardOffset = v)
                .build();

        var downEntry = eb.startDoubleField(Component.literal("Down Offset"), cfg.downOffset)
                .setDefaultValue(0.2)
                .setMin(0.0)
                .setTooltip(Component.literal("Meters below the player head to spawn particles."))
                .setSaveConsumer(v -> cfg.downOffset = v)
                .build();

        var burstEntry = eb.startIntField(Component.literal("Burst Duration (ticks)"), cfg.breathBurstDurationTicks)
                .setDefaultValue(10)
                .setMin(1)
                .setTooltip(Component.literal("How long a breath burst continues spawning particles (ticks)."))
                .setSaveConsumer(v -> cfg.breathBurstDurationTicks = v)
                .build();

        String currentHex = String.format("#%06X", (0xFFFFFF & cfg.breathColor));
        var colorEntry = eb.startStrField(Component.literal("Breath Color (hex)"), currentHex)
                .setDefaultValue("#E6F2FF")
                .setTooltip(Component.literal("RGB hex for breath particles. Accepts #RRGGBB, 0xRRGGBB, or RRGGBB."))
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

        var sizeEntry = eb.startDoubleField(Component.literal("Breath Size"), cfg.breathSize)
                .setDefaultValue(0.6)
                .setMin(0.1)
                .setTooltip(Component.literal("Particle scale for breath particles (min 0.1)."))
                .setSaveConsumer(v -> cfg.breathSize = v)
                .build();

        visualsCat.addEntry(forwardEntry);
        visualsCat.addEntry(downEntry);
        visualsCat.addEntry(burstEntry);
        visualsCat.addEntry(colorEntry);
		visualsCat.addEntry(sizeEntry);

		// --- Seasons ---
		var seasonsToggle = eb.startBooleanToggle(Component.literal("Enable Seasons"), cfg.seasonsEnabled)
				.setDefaultValue(true)
				.setTooltip(
						Component.literal("Master switch for seasonal temperature and condensation adjustments."),
						Component.literal("Requires a supported season mod for live data; otherwise uses simple cycling.")
				)
				.setSaveConsumer(v -> cfg.seasonsEnabled = v)
				.build();

		var sereneToggle = eb.startBooleanToggle(Component.literal("Input Serene Seasons"), cfg.sereneSeasonsIntegration)
				.setDefaultValue(true)
				.setTooltip(Component.literal("When enabled and Serene Seasons is installed, use its data for season-aware adjustments."))
				.setSaveConsumer(v -> cfg.sereneSeasonsIntegration = v)
				.build();

		SeasonPhase[] phases = SeasonPhase.orderedValues();
		double[] defaultTemps = ColdBreathConfig.defaultTemperatureOffsets();
		boolean[] defaultCondensation = ColdBreathConfig.defaultBreathCondensation();

		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> perSeasonTempEntries = new ArrayList<>();
		@SuppressWarnings("rawtypes")
		List<AbstractConfigListEntry> perSeasonCondensationEntries = new ArrayList<>();
		for (int i = 0; i < phases.length; i++) {
			final int index = i;
			SeasonPhase phase = phases[index];
			String display = phase.displayName();
			String label = display.isEmpty()
					? "Subseason " + (index + 1)
					: display.substring(0, 1).toUpperCase(Locale.ROOT) + display.substring(1);

			var tempEntry = eb.startDoubleField(Component.literal(label + " Temperature Offset"), cfg.seasonTemperatureOffsets[index])
					.setDefaultValue(defaultTemps[index])
					.setTooltip(Component.literal("Adjust the effective temperature during " + display + "."))
					.setMin(-1.0)
					.setMax(1.0)
					.setSaveConsumer(v -> cfg.seasonTemperatureOffsets[index] = v)
					.build();

			var condensationEntry = eb.startBooleanToggle(Component.literal(label + " Breath Condensation"), cfg.seasonBreathCondensation[index])
					.setDefaultValue(defaultCondensation[index])
					.setTooltip(Component.literal("Enable breath condensation effects during " + display + "."))
					.setSaveConsumer(v -> cfg.seasonBreathCondensation[index] = v)
					.build();

			perSeasonTempEntries.add(tempEntry);
			perSeasonCondensationEntries.add(condensationEntry);
		}

		AbstractConfigListEntry<?> perSeasonTempSub = eb.startSubCategory(Component.literal("Season Temperature"), perSeasonTempEntries).build();
		AbstractConfigListEntry<?> perSeasonCondensationSub = eb.startSubCategory(Component.literal("Season Breath Condensation"), perSeasonCondensationEntries).build();
		seasonsCat.addEntry(seasonsToggle);
		seasonsCat.addEntry(sereneToggle);

		var fabricSeasonsToggle = eb.startBooleanToggle(Component.literal("Input Fabric Seasons"), cfg.fabricSeasonsIntegration)
				.setDefaultValue(true)
				.setTooltip(Component.literal("When enabled and Fabric Seasons is installed, use its data for season-aware adjustments."))
				.setSaveConsumer(v -> cfg.fabricSeasonsIntegration = v)
				.build();

		seasonsCat.addEntry(fabricSeasonsToggle);
		seasonsCat.addEntry(perSeasonTempSub);
		seasonsCat.addEntry(perSeasonCondensationSub);

        // --- Breathing (underwater) ---
        var uwToggle = eb.startBooleanToggle(Component.literal("Enable Underwater Breaths"), cfg.underwaterEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Show bubble breaths when underwater (has air bar)."))
                .setSaveConsumer(v -> cfg.underwaterEnabled = v)
                .build();

        var uwBase = eb.startIntSlider(
                        Component.literal("Underwater Base Interval (seconds)"),
                        (int) Math.round(cfg.underwaterBaseIntervalSeconds * 10),
                        40, 200 // 4.0–20.0 s
                )
                .setDefaultValue(80) // 8.0 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Component.literal("Average delay between bubble breaths underwater (4.0–20.0 s)."))
                .setSaveConsumer(i -> cfg.underwaterBaseIntervalSeconds = i / 10.0)
                .build();

        var uwDev = eb.startIntSlider(
                        Component.literal("Underwater Deviation (seconds)"),
                        (int) Math.round(cfg.underwaterIntervalDeviationSeconds * 10),
                        0, 40 // 0.0–4.0 s
                )
                .setDefaultValue(10) // 1.0 s
                .setTextGetter(i -> Component.literal(String.format("%.1f s", i / 10.0)))
                .setTooltip(Component.literal("Random variation for underwater breaths (0.0–4.0 s)."))
                .setSaveConsumer(i -> cfg.underwaterIntervalDeviationSeconds = i / 10.0)
                .build();

        @SuppressWarnings({"rawtypes"})
        List<AbstractConfigListEntry> underwaterEntries = new ArrayList<>();
        underwaterEntries.add(uwToggle);
        underwaterEntries.add(uwBase);
        underwaterEntries.add(uwDev);
        AbstractConfigListEntry<?> underwaterSub = eb.startSubCategory(Component.literal("Underwater"), underwaterEntries).build();
        breathingCat.addEntry(underwaterSub);

        // --- Visibility ---
        var visibleCreativeEntry = eb.startBooleanToggle(Component.literal("Visible in Creative"), cfg.visibleInCreative)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Show breaths while in Creative mode."))
                .setSaveConsumer(v -> cfg.visibleInCreative = v)
                .build();

        var visibleNetherEntry = eb.startBooleanToggle(Component.literal("Visible in Nether"), cfg.visibleInNether)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Show breaths while in the Nether."))
                .setSaveConsumer(v -> cfg.visibleInNether = v)
                .build();

        var visibleEndEntry = eb.startBooleanToggle(Component.literal("Visible in End"), cfg.visibleInEnd)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Show breaths while in the End."))
                .setSaveConsumer(v -> cfg.visibleInEnd = v)
                .build();

        visibilityCat.addEntry(visibleCreativeEntry);
        visibilityCat.addEntry(visibleNetherEntry);
        visibilityCat.addEntry(visibleEndEntry);

        // --- Debug ---
        var debugEntry = eb.startBooleanToggle(Component.literal("Debug Overlay"), cfg.debugEnabled)
                .setDefaultValue(false)
                .setTooltip(
                        Component.literal("Show on-screen state and sprint blend values."),
                        Component.literal("If the on-screen overlay fails to render, use /coldbreath commands instead.")
                )
                .setSaveConsumer(v -> cfg.debugEnabled = v)
                .build();

        debugCat.addEntry(debugEntry);

        var debugCommandsEntry = eb.startBooleanToggle(Component.literal("Debug Commands"), cfg.debugCommandsEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Allow the /coldbreath client commands."))
                .setSaveConsumer(v -> cfg.debugCommandsEnabled = v)
                .build();

        debugCat.addEntry(debugCommandsEntry);

        return builder.build();
    }
}
