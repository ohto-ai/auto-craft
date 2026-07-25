package com.autocraft.overlay;

import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskExecutor;
import org.lwjgl.glfw.GLFW;

/**
 * Handles input blocking and space-key pause/resume during active crafting tasks.
 * When a task is running, all keyboard and mouse input is consumed except:
 * - SPACE (pause/resume toggle)
 * - ESC (opens game menu — allows player to access pause menu)
 */
public class OverlayInputHandler {

    private final TaskExecutor taskExecutor;
    private boolean active;

    public OverlayInputHandler(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        this.active = false;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Handle a key press. Called from mixin or event hooks.
     * @return true if the key was handled (should cancel default behavior)
     */
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!taskExecutor.hasActiveTask()) return false;

        CraftingTask task = taskExecutor.getActiveTask();
        if (task == null) return false;

        // Space key → toggle pause
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            taskExecutor.onSpacePressed();
            return true; // Consume the key
        }

        // ESC → let through (allows pausing via game menu)
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false; // Don't consume — let the game handle it
        }

        // If task is running (not paused, not stopped), block all other input
        if (taskExecutor.isRunning()) {
            return true; // Consume all other keys
        }

        return false;
    }

    /**
     * Handle mouse click. Blocks all mouse input during active task.
     * @return true if the click was blocked
     */
    public boolean onMouseClick(int button, int action, int modifiers) {
        if (!taskExecutor.isRunning()) return false;
        // Block all mouse input during active crafting
        return true;
    }

    /**
     * Handle mouse scroll. Blocks scrolling during active task.
     * @return true if scrolling was blocked
     */
    public boolean onMouseScroll(double horizontal, double vertical) {
        return taskExecutor.isRunning();
    }
}
