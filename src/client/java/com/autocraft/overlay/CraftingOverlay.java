package com.autocraft.overlay;

import com.autocraft.AutoCraftMod;
import com.autocraft.station.StationConfig;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.MaterialPlanner;
import com.autocraft.task.RetryTracker;
import com.autocraft.task.TaskExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

/**
 * Semi-transparent HUD overlay shown during crafting tasks.
 * When a task stops, fades out over 5 seconds then disappears.
 */
public class CraftingOverlay {

    private final TaskExecutor taskExecutor;
    private long stoppedAt;
    private float fadeAlpha = 1f;

    private static final int PANEL_X = 10;
    private static final int PANEL_Y = 10;
    private static final int PANEL_WIDTH = 280;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 8;
    private static final long FADE_MS = 5000;

    public CraftingOverlay(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void render(DrawContext context) {
        CraftingTask task = taskExecutor.getActiveTask();
        if (task == null) return;

        // Calculate fade: running → full, stopped → fade out
        if (task.isStopped()) {
            if (stoppedAt == 0) stoppedAt = System.currentTimeMillis();
            long elapsed = System.currentTimeMillis() - stoppedAt;
            if (elapsed >= FADE_MS) return; // fully faded
            fadeAlpha = 1f - (float) elapsed / FADE_MS;
        } else {
            stoppedAt = 0;
            fadeAlpha = 1f;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        if (textRenderer == null) return;

        int panelHeight = calcHeight(task);
        int bgAlpha = (int) (fadeAlpha * 100);
        int bgColor = bgAlpha << 24;

        // Background
        context.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_WIDTH, PANEL_Y + panelHeight, bgColor);

        int y = PANEL_Y + PADDING;
        int x = PANEL_X + PADDING;
        int white = argb(fadeAlpha, 255, 255, 255);
        int gray  = argb(fadeAlpha, 170, 170, 170);
        int green = argb(fadeAlpha, 85, 255, 85);
        int yellow= argb(fadeAlpha, 255, 255, 85);
        int red   = argb(fadeAlpha, 255, 85, 85);
        int cyan  = argb(fadeAlpha, 85, 255, 255);

        // Header
        String statusText = task.isStopped() ? "STOPPED" : task.isPaused() ? "PAUSED" : "RUNNING";
        int statusColor = task.isStopped() ? red : task.isPaused() ? yellow : green;
        context.drawText(textRenderer, "AutoCraft [" + statusText + "]", x, y, statusColor, true);
        y += LINE_HEIGHT + 2;

        context.drawText(textRenderer, "Recipe: " + task.getRecipe().getName(), x, y, cyan, true);
        y += LINE_HEIGHT;

        // Progress
        String progress = task.isInfinite()
                ? "Progress: " + task.getCompletedCount() + " (infinite)"
                : "Progress: " + task.getCompletedCount() + " / " + task.getTargetCount();
        context.drawText(textRenderer, progress, x, y, white, true);
        y += LINE_HEIGHT;

        double frac = task.getProgressFraction();
        if (frac >= 0 && !task.isInfinite()) {
            int barW = PANEL_WIDTH - 2 * PADDING;
            context.fill(x, y, x + barW, y + 6, 0xFF333333);
            if (frac > 0) context.fill(x, y, x + (int)(barW * frac), y + 6, 0xFF00AA00);
            y += 10;
        }

        String step = "Step: " + (task.getCurrentState() != null ? task.getCurrentState().getDescription() : "N/A");
        context.drawText(textRenderer, step, x, y, white, true);
        y += LINE_HEIGHT + 2;

        // Divider
        context.drawText(textRenderer, "------------------------------", x, y, gray, true);
        y += LINE_HEIGHT;

        // Input container statuses
        StationConfig station = task.getStation();
        for (int i = 0; i < station.getInputs().size(); i++) {
            BlockPos pos = station.getInputs().get(i);
            RetryTracker tracker = task.getRetryTrackers().get(pos);
            int col = green;
            String s = "OK";
            if (tracker != null && tracker.isActive()) {
                col = yellow;
                s = "WAIT " + tracker.getRemainingSeconds() + "s";
                if (tracker.isTimedOut()) { col = red; s = "TIMEOUT"; }
            }
            context.drawText(textRenderer, "Input #" + i + " " + pos.toShortString() + " [" + s + "]", x, y, col, true);
            y += LINE_HEIGHT;
        }

        // Output status
        BlockPos out = station.getOutput();
        if (out != null) {
            RetryTracker tracker = task.getRetryTrackers().get(out);
            int col = green;
            String s = "OK";
            if (tracker != null && tracker.isActive()) {
                col = yellow;
                s = "FULL/WAIT " + tracker.getRemainingSeconds() + "s";
                if (tracker.isTimedOut()) { col = red; s = "TIMEOUT"; }
            }
            context.drawText(textRenderer, "Output " + out.toShortString() + " [" + s + "]", x, y, col, true);
            y += LINE_HEIGHT;
        }

        // Materials needed
        MaterialPlanner.MaterialPlan plan = task.getCurrentMaterialPlan();
        if (plan != null && !plan.needFromContainers.isEmpty()) {
            context.drawText(textRenderer, "------------------------------", x, y, gray, true);
            y += LINE_HEIGHT;
            context.drawText(textRenderer, "Needed:", x, y, gray, true);
            y += LINE_HEIGHT;
            for (var e : plan.needFromContainers.entrySet()) {
                context.drawText(textRenderer, "  " + e.getKey().replace("minecraft:", "") + " x" + e.getValue(), x, y, white, true);
                y += LINE_HEIGHT;
            }
        }

        // Footer
        context.drawText(textRenderer, "------------------------------", x, y, gray, true);
        y += LINE_HEIGHT;
        if (!task.isInfinite() && task.getCompletedCount() > 0) {
            long elapsed = System.currentTimeMillis() - task.getStartTimeMs();
            double rate = task.getCompletedCount() / Math.max(1.0, elapsed);
            long remain = (long)((task.getTargetCount() - task.getCompletedCount()) / Math.max(0.001, rate));
            context.drawText(textRenderer, "ETA: " + fmtDuration(remain), x, y, white, true);
            y += LINE_HEIGHT;
        }
        if (task.isStopped()) {
            String reason = task.getStopReason();
            if (reason != null && reason.length() > 45) reason = reason.substring(0, 42) + "...";
            context.drawText(textRenderer, "STOPPED: " + reason, x, y, red, true);
            y += LINE_HEIGHT;
            context.drawText(textRenderer, "Use /autocraft task start to restart", x, y, gray, true);
        } else {
            context.drawText(textRenderer, task.isPaused() ? "[SPACE] Resume" : "[SPACE] Pause", x, y, gray, true);
        }
    }

    private int calcHeight(CraftingTask task) {
        int h = PADDING * 2 + LINE_HEIGHT * 5 + 2 + 10;
        h += LINE_HEIGHT * Math.max(1, task.getStation().getInputs().size());
        h += LINE_HEIGHT;
        var plan = task.getCurrentMaterialPlan();
        if (plan != null && !plan.needFromContainers.isEmpty())
            h += LINE_HEIGHT * (2 + plan.needFromContainers.size());
        h += LINE_HEIGHT * 3;
        return Math.min(h, 400);
    }

    private static int argb(float a, int r, int g, int b) {
        return ((int)(a * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    private static String fmtDuration(long ms) {
        if (ms <= 0) return "...";
        long s = ms / 1000;
        if (s < 60) return s + "s";
        return (s / 60) + "m " + (s % 60) + "s";
    }
}
