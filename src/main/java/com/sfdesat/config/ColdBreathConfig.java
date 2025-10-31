package com.sfdesat.config;

import com.sfdesat.coldbreath.season.SeasonPhase;

public class ColdBreathConfig {
	public boolean enabled = true;
	public boolean visibleInCreative = false; // show effect in creative when true
	public boolean alwaysShowBreath = false; // when true, breath appears even in warm temperatures
    public boolean visibleInNether = false; // show effect in Nether when true
    public boolean visibleInEnd = true; // show effect in End when true
	// Seconds-based settings (with 0.1s precision via UI sliders)
	public double baseIntervalSeconds = 5.0; // default 5.0s
	public double intervalDeviationSeconds = 1.0; // +/- 1.0s

	// Sprint-specific options
	public boolean sprintingIntervalsEnabled = true;
	public double sprintBaseIntervalSeconds = 3.0; // default faster when sprinting
	public double sprintIntervalDeviationSeconds = 0.5; // +/- 0.5s
	public double sprintBuildUpSeconds = 8.0; // time to ramp to sprint behavior
	public double sprintBuildDownSeconds = 12.0; // time to ramp back to normal
	public double forwardOffset = 0.3;
	public double downOffset = 0.2;
	public int breathBurstDurationTicks = 10;

	// Visuals
	public int breathColor = 0xE6F2FF; // RGB hex color for normal breath particles
	public double breathSize = 0.6; // particle size/scale for normal breath particles

	// Debug overlay
	public boolean debugEnabled = false;
	public boolean debugCommandsEnabled = true;

	// Seasons
	public boolean seasonsEnabled = true;
	public boolean sereneSeasonsIntegration = true;
	public boolean fabricSeasonsIntegration = true;
	public double[] seasonTemperatureOffsets = defaultTemperatureOffsets();
	public boolean[] seasonBreathCondensation = defaultBreathCondensation();

	// Underwater settings
	public boolean underwaterEnabled = true; // enable underwater breaths
	public double underwaterBaseIntervalSeconds = 8.0;
	public double underwaterIntervalDeviationSeconds = 1.0;

	// Altitude settings
	public boolean altitudeAdjustmentEnabled = true; // enable altitude-based temperature adjustment
	public double altitudeTemperatureRate = 0.00125; // temperature decrease per block above sea level

	// Breath condensation settings
	public boolean breathCondensationEnabled = true; // enable visible breath condensation during specific time window
	public long breathCondensationStartTick = 22500; // start of condensation time window (in ticks)
	public long breathCondensationEndTick = 1500; // end of condensation time window (in ticks)
	public double maxBreathCondensationTemperature = 0.7; // maximum temperature for condensation

	// Always breath temperature setting
	public double alwaysBreathTemperature = 0.15; // temperature threshold where breath always appears

	// Health-based breathing settings
	public boolean healthBasedBreathingEnabled = true; // enable health-based breathing intervals
	public double lowHealthIntervalSeconds = 1.0; // breathing interval when at 0 hearts (very fast)
	public double healthIntervalDeviationSeconds = 0.2; // random variation for health breathing (0.0-0.5s)

	public static double[] defaultTemperatureOffsets() {
		return new double[] {
				0.02D, 0.04D, 0.05D,
				0.08D, 0.12D, 0.10D,
				0.02D, -0.02D, -0.06D,
				-0.10D, -0.14D, -0.12D
		};
	}

	public static boolean[] defaultBreathCondensation() {
		return new boolean[] {
			true, true, false,
			false, false, false,
			false, true, true,
			true, true, true
		};
	}

	public void normalizeSeasonConfig() {
		int len = SeasonPhase.orderedValues().length;
		if (seasonTemperatureOffsets == null) {
			seasonTemperatureOffsets = defaultTemperatureOffsets();
		} else if (seasonTemperatureOffsets.length != len) {
			double[] copy = defaultTemperatureOffsets();
			for (int i = 0; i < Math.min(len, seasonTemperatureOffsets.length); i++) {
				copy[i] = seasonTemperatureOffsets[i];
			}
			seasonTemperatureOffsets = copy;
		}

		if (seasonBreathCondensation == null) {
			seasonBreathCondensation = defaultBreathCondensation();
		} else if (seasonBreathCondensation.length != len) {
			boolean[] copy = defaultBreathCondensation();
			for (int i = 0; i < Math.min(len, seasonBreathCondensation.length); i++) {
				copy[i] = seasonBreathCondensation[i];
			}
			seasonBreathCondensation = copy;
		}
	}
}