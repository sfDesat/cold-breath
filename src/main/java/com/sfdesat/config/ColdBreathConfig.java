package com.sfdesat.config;

public class ColdBreathConfig {
	public boolean enabled = true;
	public boolean visibleInCreative = false; // show effect in creative when true
	public boolean onlyInColdBiomes = true; // only show in cold biomes by default
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

	// Underwater settings
	public boolean underwaterEnabled = true; // enable underwater breaths
	public double underwaterBaseIntervalSeconds = 8.0;
	public double underwaterIntervalDeviationSeconds = 1.0;
}


