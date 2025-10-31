package com.sfdesat.coldbreath.api;

import com.sfdesat.coldbreath.breath.EnvModel;
import com.sfdesat.coldbreath.season.SeasonDetector;
import com.sfdesat.coldbreath.season.SeasonManager;
import com.sfdesat.config.ColdBreathConfig;
import com.sfdesat.config.ConfigManager;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Public API surface for external mods to query the current Cold Breath state or subscribe to breath events.
 */
public final class ColdBreathApi {

    private ColdBreathApi() {}

    /**
     * Fired whenever the mod triggers a breath emission.
     */
    public static final Event<Consumer<BreathEvent>> BREATH_EVENT = EventFactory.createArrayBacked(Consumer.class, callbacks -> event -> {
        for (Consumer<BreathEvent> callback : callbacks) {
            callback.accept(event);
        }
    });

    /**
     * Convenience wrapper to register for {@link #BREATH_EVENT}.
     */
    public static void registerBreathListener(Consumer<BreathEvent> listener) {
        BREATH_EVENT.register(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Returns the latest captured breath state for the local player, if available.
     */
    public static Optional<BreathState> currentState() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) return Optional.empty();

        ColdBreathConfig cfg = ConfigManager.get();
        ClientWorld world = client.world;
        PlayerEntity player = client.player;

        SeasonManager.refresh(world);

        EnvModel.BreathEligibility eligibility = EnvModel.checkEligibility(world, player, cfg);
        boolean underwater = player.isSubmergedInWater() && cfg.underwaterEnabled;
        float temperature = EnvModel.computeEffectiveTemperature(world, player.getBlockPos(), cfg);
        boolean condensationActive = eligibility.allowed()
                && temperature > cfg.alwaysBreathTemperature
                && !cfg.alwaysShowBreath
                && !underwater;

        return Optional.of(new BreathState(
                eligibility.allowed(),
                eligibility.reason(),
                temperature,
                condensationActive,
                underwater,
                SeasonManager.getCurrentMod(),
                SeasonManager.getCurrentPhase()
        ));
    }

    /**
     * Immutable snapshot of a breath emission.
     */
    public record BreathEvent(BreathState state) {}

    /**
     * Immutable representation of the current breath state. Note that temperature is the effective
     * temperature at the player's position, accounting for altitude and seasonal adjustments.
     */
    public record BreathState(
            boolean visible,
            String invisibleReason,
            float effectiveTemperature,
            boolean condensationActive,
            boolean underwater,
            SeasonDetector.SeasonMod seasonMod,
            com.sfdesat.coldbreath.season.SeasonPhase seasonPhase
    ) {}

    /**
     * Internal hook used by Cold Breath to notify listeners. Do not call externally.
     */
    public static void publishBreathEvent() {
        currentState().ifPresent(state -> BREATH_EVENT.invoker().accept(new BreathEvent(state)));
    }
}

