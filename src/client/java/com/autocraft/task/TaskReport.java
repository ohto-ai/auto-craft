package com.autocraft.task;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Generates a summary report when a crafting task ends, and sends it to chat.
 */
public final class TaskReport {

    private TaskReport() {}

    public static void send(CraftingTask task) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String recipeName = task.getRecipe().getName();
        String resultName = task.getRecipe().getResult().itemId;
        int yield = task.getRecipe().getResult().count;
        int produced = task.getCompletedCount();
        int target = task.getTargetCount();
        long elapsed = System.currentTimeMillis() - task.getStartTimeMs();
        String time = formatDuration(elapsed);

        boolean isChinese = isChineseLocale();

        client.player.sendMessage(Text.literal(""), false);
        client.player.sendMessage(Text.literal(
                isChinese ? "§6========== 合成报告 ==========" : "§6========== Crafting Report =========="), false);

        client.player.sendMessage(Text.literal(
                (isChinese ? "§e配方: §f" : "§eRecipe: §f") + recipeName), false);

        client.player.sendMessage(Text.literal(
                (isChinese ? "§e产物: §f" : "§eResult: §f") +
                resultName.replace("minecraft:", "") + " x" + yield + " §7(per craft)"), false);

        String progress;
        if (task.isInfinite()) {
            progress = isChinese ? (produced + " (无限)") : (produced + " (infinite)");
        } else {
            int pct = target > 0 ? (int)(100.0 * produced / target) : 0;
            progress = produced + " / " + target + " (" + pct + "%)";
        }
        client.player.sendMessage(Text.literal(
                (isChinese ? "§e产出: §f" : "§eProduced: §f") + progress), false);

        client.player.sendMessage(Text.literal(
                (isChinese ? "§e耗时: §f" : "§eTime: §f") + time), false);

        String status, statusColor;
        if (task.isComplete()) {
            status = isChinese ? "完成" : "COMPLETED";
            statusColor = "§a";
        } else if (task.getStopReason() != null && task.getStopReason().contains("timeout")) {
            status = isChinese ? "超时停止" : "STOPPED (timeout)";
            statusColor = "§c";
        } else if (task.getStopReason() != null && task.getStopReason().contains("Manually")) {
            status = isChinese ? "手动停止" : "STOPPED (manual)";
            statusColor = "§e";
        } else {
            status = isChinese ? "已停止" : "STOPPED";
            statusColor = "§c";
        }
        client.player.sendMessage(Text.literal(
                (isChinese ? "§e状态: " : "§eStatus: ") + statusColor + status), false);

        if (task.getStopReason() != null && !task.getStopReason().isEmpty()) {
            client.player.sendMessage(Text.literal(
                    (isChinese ? "§7原因: " : "§7Reason: ") + task.getStopReason()), false);
        }

        client.player.sendMessage(Text.literal(
                isChinese ? "§6=================================" : "§6================================="), false);
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        long s = ms / 1000;
        if (s < 60) return s + "s";
        long m = s / 60;
        s = s % 60;
        return m + "m " + s + "s";
    }

    private static boolean isChineseLocale() {
        String lang = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        return lang != null && lang.toLowerCase().contains("zh");
    }
}
