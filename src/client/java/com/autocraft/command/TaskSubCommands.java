package com.autocraft.command;

import com.autocraft.recipe.RecipeManager;
import com.autocraft.station.StationConfig;
import com.autocraft.station.StationManager;
import com.autocraft.task.CraftingTask;
import com.autocraft.task.TaskExecutor;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Subcommands for task control: /autocraft task ...
 */
public final class TaskSubCommands {

    private TaskSubCommands() {}

    public static LiteralArgumentBuilder<FabricClientCommandSource> start(
            TaskExecutor executor, StationManager stationManager, RecipeManager recipeManager) {
        return literal("start")
                .then(argument("station", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : stationManager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .then(argument("count", IntegerArgumentType.integer(-1))
                                .executes(ctx -> {
                                    String stationName = StringArgumentType.getString(ctx, "station");
                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                    // -1 means infinite
                                    int target = count <= 0 ? -1 : count;

                                    String error = executor.start(stationName, target);
                                    if (error != null) {
                                        ctx.getSource().sendError(Text.literal("§c" + error));
                                        return 0;
                                    }

                                    String countStr = target < 0 ? "infinite" : String.valueOf(target);
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "§aTask started: station='" + stationName + "', target=" + countStr));
                                    return 1;
                                }))
                        .executes(ctx -> {
                            // No count specified → default 64
                            String stationName = StringArgumentType.getString(ctx, "station");
                            String error = executor.start(stationName, 64);
                            if (error != null) {
                                ctx.getSource().sendError(Text.literal("§c" + error));
                                return 0;
                            }
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§aTask started: station='" + stationName + "', target=64 (default)"));
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> pause(TaskExecutor executor) {
        return literal("pause").executes(ctx -> {
            String error = executor.pause();
            if (error != null) {
                ctx.getSource().sendError(Text.literal("§c" + error));
                return 0;
            }
            ctx.getSource().sendFeedback(Text.literal("§eTask paused. Press SPACE or use /autocraft task resume."));
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> resume(TaskExecutor executor) {
        return literal("resume").executes(ctx -> {
            String error = executor.resume();
            if (error != null) {
                ctx.getSource().sendError(Text.literal("§c" + error));
                return 0;
            }
            ctx.getSource().sendFeedback(Text.literal("§aTask resumed."));
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> stop(TaskExecutor executor) {
        return literal("stop").executes(ctx -> {
            String error = executor.stop();
            if (error != null) {
                ctx.getSource().sendError(Text.literal("§c" + error));
                return 0;
            }
            ctx.getSource().sendFeedback(Text.literal("§cTask stopped."));
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> status(TaskExecutor executor) {
        return literal("status").executes(ctx -> {
            CraftingTask task = executor.getActiveTask();
            if (task == null) {
                ctx.getSource().sendFeedback(Text.literal("§7No active task."));
                return 0;
            }

            String statusLine;
            if (task.isStopped()) {
                statusLine = "§cSTOPPED — " + task.getStopReason();
            } else if (task.isPaused()) {
                statusLine = "§ePAUSED";
            } else {
                statusLine = "§aRUNNING";
            }

            String progress = task.isInfinite()
                    ? "∞"
                    : task.getCompletedCount() + " / " + task.getTargetCount();

            ctx.getSource().sendFeedback(Text.literal("§6--- Task Status ---"));
            ctx.getSource().sendFeedback(Text.literal("§7Status: " + statusLine));
            ctx.getSource().sendFeedback(Text.literal("§7Station: §f" + task.getStation().getName()));
            ctx.getSource().sendFeedback(Text.literal("§7Recipe: §f" + task.getRecipe().getName()));
            ctx.getSource().sendFeedback(Text.literal("§7Progress: §f" + progress));
            ctx.getSource().sendFeedback(Text.literal("§7Step: §f" +
                    (task.getCurrentState() != null ? task.getCurrentState().getDescription() : "N/A")));

            // Show retry status
            var retryTrackers = task.getRetryTrackers();
            if (!retryTrackers.isEmpty()) {
                for (var entry : retryTrackers.entrySet()) {
                    if (entry.getValue().isActive()) {
                        ctx.getSource().sendFeedback(Text.literal(
                                "§7Retry [" + entry.getKey().toShortString() + "]: §e" +
                                        entry.getValue().getRemainingSeconds() + "s remaining"));
                    }
                }
            }

            // Show material plan if available
            var plan = task.getCurrentMaterialPlan();
            if (plan != null && !plan.isEmpty()) {
                ctx.getSource().sendFeedback(Text.literal(
                        "§7Materials needed: §f" + plan.getUniqueItemTypesNeeded() + " types, " +
                                plan.batchSize + " crafts planned"));
            }

            return 1;
        });
    }
}
