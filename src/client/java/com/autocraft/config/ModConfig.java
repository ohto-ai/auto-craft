package com.autocraft.config;

import com.autocraft.AutoCraftMod;
import com.autocraft.util.JsonUtil;
import com.google.gson.annotations.Expose;

import java.nio.file.Path;

/**
 * Mod-level configuration with tunable timing and behavior parameters.
 */
public class ModConfig {

    @Expose private int retryCheckIntervalMs = 2000;
    @Expose private int retryTimeoutMs = 30000;
    @Expose private int minRetryChecks = 2;
    @Expose private int defaultCraftsPerBatch = 64;
    @Expose private float overlayOpacity = 0.4f;
    @Expose private int navigateTimeoutMs = 15000;
    @Expose private int containerOpenTimeoutMs = 5000;
    @Expose private int interactReachDistance = 5;
    @Expose private int batchInventoryHeadroom = 3;

    // Rate limiting & verification
    @Expose private int clickDelayTicks = 2;
    @Expose private int verifyWaitTicks = 4;
    @Expose private int maxVerifyRetries = 3;

    // --- Getters ---

    public int getRetryCheckIntervalMs() {
        return retryCheckIntervalMs;
    }

    public int getRetryTimeoutMs() {
        return retryTimeoutMs;
    }

    public int getMinRetryChecks() {
        return minRetryChecks;
    }

    public int getDefaultCraftsPerBatch() {
        return defaultCraftsPerBatch;
    }

    public float getOverlayOpacity() {
        return overlayOpacity;
    }

    public int getNavigateTimeoutMs() {
        return navigateTimeoutMs;
    }

    public int getContainerOpenTimeoutMs() {
        return containerOpenTimeoutMs;
    }

    public int getInteractReachDistance() {
        return interactReachDistance;
    }

    public int getBatchInventoryHeadroom() {
        return batchInventoryHeadroom;
    }

    public int getClickDelayTicks() {
        return clickDelayTicks;
    }

    public int getVerifyWaitTicks() {
        return verifyWaitTicks;
    }

    public int getMaxVerifyRetries() {
        return maxVerifyRetries;
    }

    // --- Persistence ---

    public void save(Path path) {
        JsonUtil.writeJson(path, this);
    }

    public static ModConfig load(Path path) {
        ModConfig loaded = JsonUtil.readJson(path, ModConfig.class);
        if (loaded != null) {
            // Merge with defaults: save back to ensure any new fields appear
            loaded.save(path);
            return loaded;
        }
        // No config file yet — create default and save
        ModConfig defaults = new ModConfig();
        defaults.save(path);
        AutoCraftMod.LOGGER.info("Created default config at {}", path);
        return defaults;
    }
}
