package com.autocraft.navigation;

import com.autocraft.AutoCraftMod;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * Simple A*-based pathfinding with basic obstacle avoidance.
 * Operates on a 2D grid (XZ plane) with height-map awareness.
 * Uses WASD-style movement simulation to approach the target.
 */
public class SimpleNavigator implements NavigationProvider {

    private static final double ARRIVAL_DISTANCE = 2.5;
    private static final double ARRIVAL_DISTANCE_SQ = ARRIVAL_DISTANCE * ARRIVAL_DISTANCE;
    private static final int MAX_PATH_LENGTH = 64;

    private BlockPos target;
    private List<BlockPos> currentPath;
    private int pathIndex;
    private int stuckTicks;
    private BlockPos lastPosition;

    @Override
    public void navigateTo(BlockPos target) {
        this.target = target;
        this.currentPath = null;
        this.pathIndex = 0;
        this.stuckTicks = 0;
        recalculatePath();
    }

    @Override
    public Result tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || target == null) return Result.FAILED;

        // Check arrival
        double distSq = player.getPos().squaredDistanceTo(target.toCenterPos());
        if (distSq <= ARRIVAL_DISTANCE_SQ) {
            cancel();
            return Result.ARRIVED;
        }

        // Check if stuck
        BlockPos currentPos = player.getBlockPos();
        if (currentPos.equals(lastPosition)) {
            stuckTicks++;
            if (stuckTicks > 60) { // 3 seconds stuck → recalculate
                recalculatePath();
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
        }
        lastPosition = currentPos;

        // Follow path
        if (currentPath != null && pathIndex < currentPath.size()) {
            BlockPos nextWaypoint = currentPath.get(pathIndex);
            Vec3d waypointCenter = nextWaypoint.toCenterPos();

            // Check if close enough to waypoint
            if (player.getPos().squaredDistanceTo(waypointCenter) < ARRIVAL_DISTANCE_SQ) {
                pathIndex++;
                if (pathIndex >= currentPath.size()) {
                    // Reached end of path — head directly to target
                    moveToward(target.toCenterPos(), player);
                }
                return Result.PATHING;
            }

            moveToward(waypointCenter, player);
        } else {
            // No path — move directly toward target
            moveToward(target.toCenterPos(), player);
        }

        return Result.PATHING;
    }

    @Override
    public void cancel() {
        this.target = null;
        this.currentPath = null;
        this.pathIndex = 0;
        // Stop movement
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.options != null) {
            client.options.forwardKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
        }
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available as fallback
    }

    private void recalculatePath() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || target == null) return;

        this.currentPath = findPath(client.player.getBlockPos(), target);
        this.pathIndex = 0;
    }

    /**
     * Simple A* on the XZ plane with basic vertical consideration.
     */
    private List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return null;

        // For very short distances, skip pathfinding
        if (start.getSquaredDistance(goal) < 9) return null;

        // Simple greedy path: move in X then Z toward target
        // This is a simplified approach — a full A* would be more complex
        // but suffices for basic crafting station navigation
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = start;
        int maxSteps = MAX_PATH_LENGTH;

        while (!current.equals(goal) && path.size() < maxSteps) {
            int dx = Integer.compare(goal.getX(), current.getX());
            int dz = Integer.compare(goal.getZ(), current.getZ());

            // Prefer moving in the axis with greater difference
            BlockPos next;
            if (Math.abs(goal.getX() - current.getX()) > Math.abs(goal.getZ() - current.getZ())) {
                next = current.add(dx, 0, 0);
            } else {
                next = current.add(0, 0, dz);
            }

            // Check if the path is walkable at the correct Y level
            next = findWalkableY(world, next);
            if (next == null) {
                // Try the other axis
                if (dx != 0) {
                    next = current.add(0, 0, dz);
                } else if (dz != 0) {
                    next = current.add(dx, 0, 0);
                }
                next = findWalkableY(world, next);
                if (next == null) break;
            }

            path.add(next);
            current = next;
        }

        return path.isEmpty() ? null : path;
    }

    /**
     * Find a walkable Y level near the given XZ position.
     */
    private BlockPos findWalkableY(World world, BlockPos pos) {
        if (world == null) return pos;

        // Check around player's current Y level
        int playerY = MinecraftClient.getInstance().player != null
                ? MinecraftClient.getInstance().player.getBlockY() : pos.getY();

        for (int dy = 0; dy <= 2; dy++) {
            BlockPos check = new BlockPos(pos.getX(), playerY + dy, pos.getZ());
            BlockState state = world.getBlockState(check);
            BlockState above = world.getBlockState(check.up());

            if (!state.isSolidBlock(world, check) && !above.isSolidBlock(world, check.up())) {
                // Also check there's ground below
                BlockState below = world.getBlockState(check.down());
                if (below.isSolidBlock(world, check.down())) {
                    return check;
                }
            }
        }
        // Fallback: return original position
        return pos;
    }

    private void moveToward(Vec3d targetPos, ClientPlayerEntity player) {
        if (player == null) return;

        Vec3d playerPos = player.getPos();
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.2) return;

        // Calculate direction and simulate key presses
        float yaw = player.getYaw();
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yawDiff = ((targetYaw - yaw + 540) % 360) - 180;

        // Turn toward target
        player.setYaw(yaw + Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), 10));

        // Move forward if facing roughly the right direction
        if (Math.abs(yawDiff) < 30) {
            MinecraftClient.getInstance().options.forwardKey.setPressed(true);
        } else {
            MinecraftClient.getInstance().options.forwardKey.setPressed(false);
        }

        // Jump if needed (target is above us)
        if (targetPos.y > playerPos.y + 0.5) {
            MinecraftClient.getInstance().options.jumpKey.setPressed(true);
        } else {
            MinecraftClient.getInstance().options.jumpKey.setPressed(false);
        }

        // Sprint for efficiency
        if (dist > 5 && Math.abs(yawDiff) < 10) {
            MinecraftClient.getInstance().options.sprintKey.setPressed(true);
        }
    }
}
