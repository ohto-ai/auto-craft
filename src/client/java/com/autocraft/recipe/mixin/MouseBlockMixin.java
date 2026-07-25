package com.autocraft.recipe.mixin;

import com.autocraft.AutoCraftMod;
import com.autocraft.task.TaskExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks mouse clicks and scroll when an autocraft task is actively running.
 */
@Mixin(Mouse.class)
public class MouseBlockMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (AutoCraftMod.getInstance().getTaskExecutor().isRunning()) {
            ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (AutoCraftMod.getInstance().getTaskExecutor().isRunning()) {
            ci.cancel();
        }
    }
}
