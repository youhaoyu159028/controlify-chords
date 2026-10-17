package dev.youhaoyu.controlifychords;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint for the standalone Controlify Chords addon.
 *
 * Input interception is intentionally not wired yet: the next implementation
 * step must be verified against the exact Controlify API and controller-state
 * lifecycle before it can safely suppress or synthesize inputs.
 */
public final class ControlifyChords implements ClientModInitializer {
    public static final String MOD_ID = "controlify_chords";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Controlify Chords initialized. Input handling is not implemented yet.");
    }
}
