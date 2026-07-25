package com.autocraft.overlay;

import com.autocraft.AutoCraftMod;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.station.StationConfig;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.MaterialPlanner;
import com.autocraft.task.RetryTracker;
import com.autocraft.task.TaskExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.BlockPos;

/**
 * Renders the semi-transparent HUD overlay during active crafting tasks.
 * Blocks player input while visible (input blocking is handled by OverlayInputHandler).
 */
public class CraftingOverlay {

    private final TaskExecutor taskExecutor;

    private static final int PANEL_X = 10;
    private static final int PANEL_Y = 10;
    private static final int PANEL_WIDTH = 280;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 8;

    public CraftingOverlay(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * Main render call from HudRenderCallback.
     */
    public void render(DrawContext context) {
        CraftingTask task = taskExecutor.getActiveTask();
        if (task == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        if (textRenderer == null) return;

        int panelHeight = calculatePanelHeight(task);
        float opacity = AutoCraftMod.getInstance().getConfig().getOverlayOpacity();
        int bgColor = ((int)(opacity * 255) << 24); // ARGB: alpha only

        // Draw semi-transparent background
        context.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_WIDTH, PANEL_Y + panelHeight, bgColor);

        int y = PANEL_Y + PADDING;
        int x = PANEL_X + PADDING;
        int white = 0xFFFFFFFF;
        int gray = 0xFFAAAAAA;
        int green = 0xFF55FF55;
        int yellow = 0xFFFFFF55;
        int red = 0xFFFF5555;
        int cyan = 0xFF55FFFF;

        // --- Header: Status + Recipe ---
        String statusText = getStatusText(task);
        int statusColor = getStatusColor(task);
        String header = "AutoCraft [" + statusText + "]";
        context.drawText(textRenderer, header, x, y, statusColor, true);
        y += LINE_HEIGHT + 2;

        // Recipe name
        String recipeText = "Recipe: " + task.getRecipe().getName();
        context.drawText(textRenderer, recipeText, x, y, cyan, true);
        y += LINE_HEIGHT;

        // --- Progress ---
        String progress;
        if (task.isInfinite()) {
            progress = "Progress: " + task.getCompletedCount() + " (infinite)";
        } else {
            progress = "Progress: " + task.getCompletedCount() + " / " + task.getTargetCount();
        }
        context.drawText(textRenderer, progress, x, y, white, true);
        y += LINE_HEIGHT;

        // Progress bar
        double frac = task.getProgressFraction();
        if (frac >= 0) {
            int barWidth = PANEL_WIDTH - 2 * PADDING;
            int barHeight = 6;
            context.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);
            if (frac > 0) {
                int filledWidth = (int)(barWidth * frac);
                context.fill(x, y, x + filledWidth, y + barHeight, 0xFF00AA00);
            }
            y += barHeight + 4;
        }

        // --- Current step ---
        String stepText = "Step: " + (task.getCurrentState() != null
                ? task.getCurrentState().getDescription() : "N/A");
        context.drawText(textRenderer, stepText, x, y, white, true);
        y += LINE_HEIGHT + 2;

        // --- Separator ---
        context.drawText(textRenderer, "§7─────────────────────────", x, y, gray, true);
        y += LINE_HEIGHT;

        // --- Input container status ---
        StationConfig station = task.getStation();
        for (int i = 0; i < station.getInputs().size(); i++) {
            BlockPos input = station.getInputs().get(i);
            RetryTracker tracker = task.getRetryTrackers().get(input);

            int inputColor = green;
            String status = "OK";
            if (tracker != null && tracker.isActive()) {
                inputColor = yellow;
                status = "WAIT " + tracker.getRemainingSeconds() + "s";
                if (tracker.isTimedOut()) {
                    inputColor = red;
                    status = "TIMEOUT";
                }
            }

            String inputText = "Input #" + i + " " + input.toShortString() + " [" + status + "]";
            context.drawText(textRenderer, inputText, x, y, inputColor, true);
            y += LINE_HEIGHT;
        }

        // --- Output container status ---
        BlockPos output = station.getOutput();
        if (output != null) {
            RetryTracker tracker = task.getRetryTrackers().get(output);
            int outputColor = green;
            String outputStatus = "OK";
            if (tracker != null && tracker.isActive()) {
                outputColor = yellow;
                outputStatus = "FULL/WAIT " + tracker.getRemainingSeconds() + "s";
                if (tracker.isTimedOut()) {
                    outputColor = red;
                    outputStatus = "TIMEOUT";
                }
            }
            String outputText = "Output " + output.toShortString() + " [" + outputStatus + "]";
            context.drawText(textRenderer, outputText, x, y, outputColor, true);
            y += LINE_HEIGHT;
        }

        // --- Material plan ---
        MaterialPlanner.MaterialPlan plan = task.getCurrentMaterialPlan();
        if (plan != null && !plan.needFromContainers.isEmpty()) {
            context.drawText(textRenderer, "§7─────────────────────────", x, y, gray, true);
            y += LINE_HEIGHT;
            context.drawText(textRenderer, "Needed from containers:", x, y, gray, true);
            y += LINE_HEIGHT;
            for (var entry : plan.needFromContainers.entrySet()) {
                // Truncate "minecraft:" prefix for display
                String itemName = entry.getKey().replace("minecraft:", "");
                String line = "  " + itemName + " x" + entry.getValue();
                context.drawText(textRenderer, line, x, y, white, true);
                y += LINE_HEIGHT;
            }
        }

        // --- Retry / ETA ---
        context.drawText(textRenderer, "§7─────────────────────────", x, y, gray, true);
        y += LINE_HEIGHT;

        // ETA
        if (!task.isInfinite() && task.getCompletedCount() > 0) {
            long elapsed = System.currentTimeMillis() - task.getStartTimeMs();
            double rate = (double) task.getCompletedCount() / Math.max(1, elapsed);
            long remaining = (long)((task.getTargetCount() - task.getCompletedCount()) / Math.max(0.001, rate));
            String eta = "ETA: " + formatDuration(remaining);
            context.drawText(textRenderer, eta, x, y, white, true);
            y += LINE_HEIGHT;
        }

        // Controls
        if (task.isPaused()) {
            context.drawText(textRenderer, "§e[SPACE] Resume", x, y, yellow, true);
        } else if (task.isStopped()) {
            String reason = task.getStopReason();
            if (reason != null && reason.length() > 50) reason = reason.substring(0, 47) + "...";
            context.drawText(textRenderer, "§cSTOPPED: " + reason, x, y, red, true);
            y += LINE_HEIGHT;
            context.drawText(textRenderer, "§7Use /autocraft task start to restart", x, y, gray, true);
        } else {
            context.drawText(textRenderer, "§7[SPACE] Pause  [ESC] Menu", x, y, gray, true);
        }
    }

    private int calculatePanelHeight(CraftingTask task) {
        int height = PADDING * 2; // padding top + bottom
        height += LINE_HEIGHT * 2 + 2; // header + recipe
        height += LINE_HEIGHT * 2 + 4; // progress text + bar
        height += LINE_HEIGHT + 2; // step
        height += LINE_HEIGHT; // separator

        // Input status lines
        height += LINE_HEIGHT * Math.max(1, task.getStation().getInputs().size());

        // Output status
        height += LINE_HEIGHT;

        // Material plan (if any)
        MaterialPlanner.MaterialPlan plan = task.getCurrentMaterialPlan();
        if (plan != null && !plan.needFromContainers.isEmpty()) {
            height += LINE_HEIGHT * 2; // separator + header
            height += LINE_HEIGHT * plan.needFromContainers.size();
        }

        // Bottom section
        height += LINE_HEIGHT * 3; // separator + ETA + controls

        return Math.min(height, 400); // cap at reasonable height
    }

    private String getStatusText(CraftingTask task) {
        if (task.isStopped()) return "STOPPED";
        if (task.isPaused()) return "PAUSED";
        return "RUNNING";
    }

    private int getStatusColor(CraftingTask task) {
        if (task.isStopped()) return 0xFFFF5555;
        if (task.isPaused()) return 0xFFFFFF55;
        return 0xFF55FF55;
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "calculating...";
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + "m " + seconds + "s";
    }
}
