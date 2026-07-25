package com.autocraft.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Player-related utility functions.
 */
public final class PlayerHelper {

    private PlayerHelper() {}

    /**
     * Get the block the player is currently looking at, or null if none.
     */
    @Nullable
    public static BlockPos getLookedAtBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return null;

        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    /**
     * Check if the player is within interact distance of a block.
     */
    public static boolean isWithinReach(BlockPos pos, double reachDistance) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;

        Vec3d eye = client.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        return eye.squaredDistanceTo(center) <= reachDistance * reachDistance;
    }

    /**
     * Squared distance from the player's eyes to the center of a block.
     */
    public static double squaredDistanceTo(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return Double.MAX_VALUE;

        Vec3d eye = client.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        return eye.squaredDistanceTo(center);
    }

    /**
     * Get client player, or null.
     */
    @Nullable
    public static PlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }

    /**
     * Get client world, or null.
     */
    @Nullable
    public static World getWorld() {
        return MinecraftClient.getInstance().world;
    }

    /**
     * Get the MinecraftClient instance.
     */
    public static MinecraftClient getClient() {
        return MinecraftClient.getInstance();
    }
}
