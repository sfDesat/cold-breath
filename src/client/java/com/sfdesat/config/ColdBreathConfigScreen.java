package com.sfdesat.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public final class ColdBreathConfigScreen {
	private ColdBreathConfigScreen() {}

	public static Screen create(Screen parent) {
		ColdBreathConfig cfg = ConfigManager.get();
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Text.literal("Cold Breath Config"))
			.setSavingRunnable(ConfigManager::save);

		ConfigCategory general = builder.getOrCreateCategory(Text.literal("Cold Breath"));
		ConfigEntryBuilder eb = builder.entryBuilder();

		// Subcategories with tooltips
		var enabledEntry = eb.startBooleanToggle(Text.literal("Enabled"), cfg.enabled)
			.setDefaultValue(true)
			.setTooltip(Text.literal("Master switch for Cold Breath feature."))
			.setSaveConsumer(val -> cfg.enabled = val)
			.build();

		var onlyColdEntry = eb.startBooleanToggle(Text.literal("Only in Cold Biomes"), cfg.onlyInColdBiomes)
			.setDefaultValue(true)
			.setTooltip(
				Text.literal("If enabled, breaths only appear in cold/snowy biomes."),
				Text.literal("Overworld only. Nether/End visibility is controlled by 'Visible in Nether' and 'Visible in End'.")
			)
			.setSaveConsumer(val -> cfg.onlyInColdBiomes = val)
			.build();

		var baseIntervalEntry = eb.startIntSlider(Text.literal("Base Interval (seconds)"), (int)Math.round(cfg.baseIntervalSeconds * 10), 20, 100)
			.setDefaultValue(50)
			.setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
			.setTooltip(Text.literal("Average delay between breaths while not sprinting (range: 2.0–10.0s)."))
			.setSaveConsumer(i -> cfg.baseIntervalSeconds = i / 10.0)
			.build();

		var baseDevEntry = eb.startIntSlider(Text.literal("Deviation (seconds)"), (int)Math.round(cfg.intervalDeviationSeconds * 10), 0, 20)
			.setDefaultValue(10)
			.setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
			.setTooltip(Text.literal("Random variation added/subtracted from base interval (range: 0.0–2.0s)."))
			.setSaveConsumer(i -> cfg.intervalDeviationSeconds = i / 10.0)
			.build();


        // Put these entries directly under the main category (no subcategory)
        general.addEntry(enabledEntry);
        general.addEntry(onlyColdEntry);
        general.addEntry(baseIntervalEntry);
        general.addEntry(baseDevEntry);

		var sprintToggleEntry = eb.startBooleanToggle(Text.literal("Enable Sprinting-specific Intervals"), cfg.sprintingIntervalsEnabled)
			.setDefaultValue(true)
			.setTooltip(Text.literal("Use separate timing while sprinting, with smooth ramp in/out."))
			.setSaveConsumer(val -> cfg.sprintingIntervalsEnabled = val)
			.build();

		var sprintBaseEntry = eb.startIntSlider(Text.literal("Sprinting Base Interval (seconds)"), (int)Math.round(cfg.sprintBaseIntervalSeconds * 10), 10, 80)
			.setDefaultValue(30)
			.setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
			.setTooltip(Text.literal("Average delay between breaths while sprinting (range: 1.0–8.0s)."))
			.setSaveConsumer(i -> cfg.sprintBaseIntervalSeconds = i / 10.0)
			.build();

		var sprintDevEntry = eb.startIntSlider(Text.literal("Sprinting Deviation (seconds)"), (int)Math.round(cfg.sprintIntervalDeviationSeconds * 10), 0, 10)
			.setDefaultValue(5)
			.setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
			.setTooltip(Text.literal("Random variation while sprinting (range: 0.0–1.0s)."))
			.setSaveConsumer(i -> cfg.sprintIntervalDeviationSeconds = i / 10.0)
			.build();

		var sprintUpEntry = eb.startIntSlider(Text.literal("Sprint Build-up (seconds)"), (int)Math.round(cfg.sprintBuildUpSeconds * 10), 0, 300)
			.setDefaultValue(80)
			.setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
			.setTooltip(Text.literal("Time to ramp from normal to sprint breathing cadence (range: 0.0–30.0s)."))
			.setSaveConsumer(i -> cfg.sprintBuildUpSeconds = i / 10.0)
			.build();

		var sprintDownEntry = eb.startIntSlider(Text.literal("Sprint Build-down (seconds)"), (int)Math.round(cfg.sprintBuildDownSeconds * 10), 0, 300)
			.setDefaultValue(120)
			.setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
			.setTooltip(Text.literal("Time to ramp from sprint back to normal cadence (range: 0.0–30.0s)."))
			.setSaveConsumer(i -> cfg.sprintBuildDownSeconds = i / 10.0)
			.build();

        List<AbstractConfigListEntry> sprintEntries = Arrays.asList(
                (AbstractConfigListEntry) sprintToggleEntry,
                (AbstractConfigListEntry) sprintBaseEntry,
                (AbstractConfigListEntry) sprintDevEntry,
                (AbstractConfigListEntry) sprintUpEntry,
                (AbstractConfigListEntry) sprintDownEntry
        );
        var sprintSub = eb.startSubCategory(Text.literal("Sprinting"), sprintEntries).build();

		var forwardEntry = eb.startDoubleField(Text.literal("Forward Offset"), cfg.forwardOffset)
			.setDefaultValue(0.3)
			.setMin(0.0)
			.setTooltip(Text.literal("Meters in front of the player head to spawn particles."))
			.setSaveConsumer(val -> cfg.forwardOffset = val)
			.build();

		var downEntry = eb.startDoubleField(Text.literal("Down Offset"), cfg.downOffset)
			.setDefaultValue(0.2)
			.setMin(0.0)
			.setTooltip(Text.literal("Meters below the player head to spawn particles."))
			.setSaveConsumer(val -> cfg.downOffset = val)
			.build();

		var burstEntry = eb.startIntField(Text.literal("Burst Duration (ticks)"), cfg.breathBurstDurationTicks)
			.setDefaultValue(10)
			.setMin(1)
			.setTooltip(Text.literal("How long a breath burst continues spawning particles (in ticks)."))
			.setSaveConsumer(val -> cfg.breathBurstDurationTicks = val)
			.build();

        List<AbstractConfigListEntry> visualEntries = new ArrayList<>(Arrays.asList(
                (AbstractConfigListEntry) forwardEntry,
                (AbstractConfigListEntry) downEntry,
                (AbstractConfigListEntry) burstEntry
        ));

        // Visuals: color (hex text) and size for normal breaths
        String currentHex = String.format("#%06X", (0xFFFFFF & cfg.breathColor));
        var colorEntry = eb.startStrField(Text.literal("Breath Color (hex)"), currentHex)
            .setDefaultValue("#E6F2FF")
            .setTooltip(Text.literal("RGB hex for normal breath particles. Accepted: #RRGGBB, 0xRRGGBB, or RRGGBB."))
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
			.setTooltip(Text.literal("Particle size/scale for normal breath particles (min: 0.1)."))
            .setSaveConsumer(val -> cfg.breathSize = val)
            .build();

        visualEntries.add((AbstractConfigListEntry) colorEntry);
        visualEntries.add((AbstractConfigListEntry) sizeEntry);

		var debugEntry = eb.startBooleanToggle(Text.literal("Debug Overlay"), cfg.debugEnabled)
			.setDefaultValue(false)
			.setTooltip(Text.literal("Show on-screen state and sprint blend values."))
			.setSaveConsumer(val -> cfg.debugEnabled = val)
			.build();

        // Debug toggle will be added at bottom, not inside a subcategory

        // --- Underwater ---
        var uwToggle = eb.startBooleanToggle(Text.literal("Enable Underwater Breaths"), cfg.underwaterEnabled)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Show bubble breaths when the player is underwater (has air bar)."))
            .setSaveConsumer(v -> cfg.underwaterEnabled = v)
            .build();

        var uwBase = eb.startIntSlider(Text.literal("Underwater Base Interval (seconds)"), (int)Math.round(cfg.underwaterBaseIntervalSeconds * 10), 40, 200)
			.setDefaultValue(80)
            .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
            .setTooltip(Text.literal("Average delay between bubble breaths underwater (range: 4.0–20.0s)."))
            .setSaveConsumer(i -> cfg.underwaterBaseIntervalSeconds = i / 10.0)
            .build();

        var uwDev = eb.startIntSlider(Text.literal("Underwater Deviation (seconds)"), (int)Math.round(cfg.underwaterIntervalDeviationSeconds * 10), 0, 40)
			.setDefaultValue(10)
            .setTextGetter(i -> Text.literal(String.format("%.1f s", i / 10.0)))
            .setTooltip(Text.literal("Random variation for underwater breaths (range: 0.0–4.0s)."))
            .setSaveConsumer(i -> cfg.underwaterIntervalDeviationSeconds = i / 10.0)
            .build();

        List<AbstractConfigListEntry> uwEntries = Arrays.asList(
            (AbstractConfigListEntry) uwToggle,
            (AbstractConfigListEntry) uwBase,
            (AbstractConfigListEntry) uwDev
        );
        var underwaterSub = eb.startSubCategory(Text.literal("Underwater"), uwEntries).build();

		general.addEntry(sprintSub);
		general.addEntry(underwaterSub);

        // Visibility category
        var visibleCreativeEntry = eb.startBooleanToggle(Text.literal("Visible in Creative"), cfg.visibleInCreative)
            .setDefaultValue(false)
            .setTooltip(Text.literal("Show breaths while in Creative mode."))
            .setSaveConsumer(val -> cfg.visibleInCreative = val)
            .build();

        var visibleNetherEntry = eb.startBooleanToggle(Text.literal("Visible in Nether"), cfg.visibleInNether)
			.setDefaultValue(false)
			.setTooltip(Text.literal("Show breaths while in the Nether."))
			.setSaveConsumer(val -> cfg.visibleInNether = val)
			.build();

		var visibleEndEntry = eb.startBooleanToggle(Text.literal("Visible in End"), cfg.visibleInEnd)
			.setDefaultValue(true)
			.setTooltip(Text.literal("Show breaths while in the End."))
			.setSaveConsumer(val -> cfg.visibleInEnd = val)
			.build();

        var dimensionsEntries = Arrays.asList(
            (AbstractConfigListEntry) visibleCreativeEntry,
            (AbstractConfigListEntry) visibleNetherEntry,
            (AbstractConfigListEntry) visibleEndEntry
        );
        var dimensionsSub = eb.startSubCategory(Text.literal("Visibility"), dimensionsEntries).build();

        // Visuals
        var visualsSub = eb.startSubCategory(Text.literal("Visuals"), visualEntries).build();
        general.addEntry(visualsSub);
        general.addEntry(dimensionsSub);

        // Place debug toggle at the bottom
        general.addEntry(debugEntry);

		return builder.build();
	}
}


