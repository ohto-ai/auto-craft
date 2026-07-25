package com.autocraft.navigation;

import com.autocraft.AutoCraftMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * Baritone API integration for advanced pathfinding.
 * Uses reflection to avoid hard compile-time dependency on Baritone.
 * Falls back gracefully if Baritone is not installed.
 */
public class BaritoneNavigator implements NavigationProvider {

    private boolean available;
    private boolean initialized;
    private BlockPos target;

    // Baritone API references (loaded via reflection)
    private Object baritoneInstance;
    private Object customGoalProcess;

    public BaritoneNavigator() {
        this.available = false;
        this.initialized = false;
        checkAvailability();
    }

    private void checkAvailability() {
        this.available = FabricLoader.getInstance().isModLoaded("baritone");
        if (this.available) {
            AutoCraftMod.LOGGER.info("Baritone detected — will use for navigation");
        } else {
            AutoCraftMod.LOGGER.info("Baritone not installed — using simple A* navigation");
        }
    }

    @Override
    public void navigateTo(BlockPos target) {
        this.target = target;

        if (!available || !initializeBaritone()) {
            return;
        }

        try {
            // Use reflection to call Baritone API:
            // BaritoneAPI.getProvider().getPrimaryBaritone()
            //     .getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z));

            Class<?> baritoneApiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = baritoneApiClass.getMethod("getProvider").invoke(null);
            Object primaryBaritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object goalProcess = primaryBaritone.getClass().getMethod("getCustomGoalProcess").invoke(primaryBaritone);

            Class<?> goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
            Object goalBlock = goalBlockClass.getConstructor(
                    int.class, int.class, int.class).newInstance(
                    target.getX(), target.getY(), target.getZ());

            goalProcess.getClass().getMethod("setGoalAndPath", Class.forName(
                    "baritone.api.pathing.goals.Goal")).invoke(goalProcess, goalBlock);

            this.customGoalProcess = goalProcess;
            AutoCraftMod.LOGGER.debug("Baritone navigating to {}", target.toShortString());

        } catch (Exception e) {
            AutoCraftMod.LOGGER.warn("Failed to invoke Baritone API: {}", e.getMessage());
            this.available = false;
        }
    }

    @Override
    public Result tick() {
        if (!available || target == null) return Result.UNAVAILABLE;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return Result.FAILED;

        // Check arrival
        if (client.player.getPos().squaredDistanceTo(target.toCenterPos()) <= 6.25) { // 2.5^2
            cancel();
            return Result.ARRIVED;
        }

        // Check if Baritone is still pathing
        try {
            if (customGoalProcess != null) {
                Object goal = customGoalProcess.getClass().getMethod("getGoal").invoke(customGoalProcess);
                if (goal == null) {
                    return Result.ARRIVED; // No active goal — we arrived or it was cancelled
                }
            }

            Object pathingBehavior = baritoneInstance != null
                    ? baritoneInstance.getClass().getMethod("getPathingBehavior").invoke(baritoneInstance)
                    : null;
            if (pathingBehavior != null) {
                boolean pathing = (boolean) pathingBehavior.getClass()
                        .getMethod("isPathing").invoke(pathingBehavior);
                return pathing ? Result.PATHING : Result.ARRIVED;
            }
        } catch (Exception e) {
            AutoCraftMod.LOGGER.debug("Baritone tick error: {}", e.getMessage());
            return Result.FAILED;
        }

        return Result.PATHING;
    }

    @Override
    public void cancel() {
        if (!available || customGoalProcess == null) return;

        try {
            customGoalProcess.getClass().getMethod("onLostControl").invoke(customGoalProcess);
            customGoalProcess = null;
            target = null;
        } catch (Exception e) {
            AutoCraftMod.LOGGER.debug("Baritone cancel error: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    private boolean initializeBaritone() {
        if (initialized) return available;
        initialized = true;

        try {
            Class<?> baritoneApiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = baritoneApiClass.getMethod("getProvider").invoke(null);
            this.baritoneInstance = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            return available;
        } catch (Exception e) {
            AutoCraftMod.LOGGER.warn("Baritone initialization failed: {}", e.getMessage());
            this.available = false;
            return false;
        }
    }
}
