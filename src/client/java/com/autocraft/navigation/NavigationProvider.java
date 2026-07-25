package com.autocraft.navigation;

import net.minecraft.util.math.BlockPos;

/**
 * Interface for navigation implementations (Baritone or Simple A*).
 */
public interface NavigationProvider {

    enum Result {
        /** Successfully arrived at destination. */
        ARRIVED,
        /** Still pathfinding / moving toward target. */
        PATHING,
        /** Pathfinding failed — no path found or navigation error. */
        FAILED,
        /** Navigation provider is unavailable (e.g., Baritone not installed). */
        UNAVAILABLE
    }

    /**
     * Start navigating toward a target block position.
     */
    void navigateTo(BlockPos target);

    /**
     * Called every tick to update navigation progress.
     */
    Result tick();

    /**
     * Cancel the current navigation.
     */
    void cancel();

    /**
     * Whether this navigation provider is currently available.
     */
    boolean isAvailable();
}
