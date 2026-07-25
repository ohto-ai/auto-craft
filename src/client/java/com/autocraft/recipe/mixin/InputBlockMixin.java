package com.autocraft.recipe.mixin;

import com.autocraft.AutoCraftMod;
import com.autocraft.task.TaskExecutor;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks keyboard input when an autocraft task is running (not paused).
 * Only allows SPACE (pause/resume) and ESC (game menu) through.
 */
@Mixin(Keyboard.class)
public class InputBlockMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TaskExecutor executor = AutoCraftMod.getInstance().getTaskExecutor();
        if (!executor.isRunning()) return; // Only block when actively running (not paused, not stopped)

        // Allow game menu
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
            return;
        }

        // Space toggles pause
        if (key == GLFW.GLFW_KEY_SPACE && action == GLFW.GLFW_RELEASE) {
            executor.onSpacePressed();
            ci.cancel();
            return;
        }

        // Block everything else
        ci.cancel();
    }
}
