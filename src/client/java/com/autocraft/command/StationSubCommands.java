package com.autocraft.command;

import com.autocraft.AutoCraftMod;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.recipe.RecipeManager;
import com.autocraft.station.StationConfig;
import com.autocraft.station.StationManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Subcommands for crafting station management: /autocraft station ...
 */
public final class StationSubCommands {

    private StationSubCommands() {}

    public static LiteralArgumentBuilder<FabricClientCommandSource> create(StationManager manager) {
        return literal("create")
                .then(argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            if (manager.create(name)) {
                                ctx.getSource().sendFeedback(Text.literal(
                                        "§aStation '" + name + "' created. Use:\n" +
                                                "  §e/autocraft station set-workbench " + name + "§a (look at workbench)\n" +
                                                "  §e/autocraft station add-input " + name + "§a (look at input chest)\n" +
                                                "  §e/autocraft station set-output " + name + "§a (look at output chest)\n" +
                                                "  §e/autocraft station bind-recipe " + name + " <recipe>"));
                            } else {
                                ctx.getSource().sendError(Text.literal(
                                        "§cStation '" + name + "' already exists."));
                            }
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> delete(StationManager manager) {
        return literal("delete")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : manager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            AutoCraftMod.getInstance().getStationHighlighter().clear();
                            if (manager.delete(name)) {
                                ctx.getSource().sendFeedback(Text.literal("§aStation '" + name + "' deleted."));
                            } else {
                                ctx.getSource().sendError(Text.literal("§cStation '" + name + "' not found."));
                            }
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> list(StationManager manager) {
        return literal("list").executes(ctx -> {
            var stations = manager.getAll();
            if (stations.isEmpty()) {
                ctx.getSource().sendFeedback(Text.literal("§7No stations configured."));
                return 0;
            }
            ctx.getSource().sendFeedback(Text.literal("§6--- Crafting Stations (" + stations.size() + ") ---"));
            for (StationConfig s : stations) {
                ctx.getSource().sendFeedback(Text.literal(
                        "§e  " + s.getName() + " §7| recipe=" + s.getBoundRecipeName() +
                                " | inputs=" + s.getInputs().size() +
                                " | " + (s.isComplete() ? "§a✓ ready" : "§c✗ incomplete")));
            }
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> show(StationManager manager) {
        return literal("show")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : manager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            StationConfig s = manager.getByName(name);
                            if (s == null) {
                                ctx.getSource().sendError(Text.literal("§cStation '" + name + "' not found."));
                                return 0;
                            }
                            ctx.getSource().sendFeedback(Text.literal("§6Station: §e" + s.getName()));
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§7  Workbench: §f" + fmtPos(s.getWorkbench())));
                            for (int i = 0; i < s.getInputs().size(); i++) {
                                ctx.getSource().sendFeedback(Text.literal(
                                        "§7  Input #" + i + ": §f" + fmtPos(s.getInputs().get(i))));
                            }
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§7  Output: §f" + fmtPos(s.getOutput())));
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§7  Recipe: §f" + s.getBoundRecipeName()));
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> setWorkbench(StationManager manager) {
        return literal("set-workbench")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : manager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            StationConfig s = manager.getByName(name);
                            if (s == null) {
                                ctx.getSource().sendError(Text.literal("§cStation not found."));
                                return 0;
                            }
                            BlockPos lookedAt = getLookedAtBlock();
                            if (lookedAt == null) {
                                ctx.getSource().sendError(Text.literal("§cLook at a crafting table to bind it."));
                                return 0;
                            }
                            s.setWorkbench(lookedAt);
                            manager.save();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§aWorkbench set to " + fmtPos(lookedAt) + " for station '" + name + "'."));
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> addInput(StationManager manager) {
        return literal("add-input")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : manager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            StationConfig s = manager.getByName(name);
                            if (s == null) {
                                ctx.getSource().sendError(Text.literal("§cStation not found."));
                                return 0;
                            }
                            BlockPos lookedAt = getLookedAtBlock();
                            if (lookedAt == null) {
                                ctx.getSource().sendError(Text.literal("§cLook at a container to add as input."));
                                return 0;
                            }
                            s.addInput(lookedAt);
                            manager.save();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§aInput container #" + (s.getInputs().size() - 1) +
                                            " added at " + fmtPos(lookedAt) + " for station '" + name + "'."));
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> removeInput(StationManager manager) {
        return literal("remove-input")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : manager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .then(argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    StationConfig s = manager.getByName(name);
                                    if (s == null) {
                                        ctx.getSource().sendError(Text.literal("§cStation not found."));
                                        return 0;
                                    }
                                    if (index >= s.getInputs().size()) {
                                        ctx.getSource().sendError(Text.literal(
                                                "§cInvalid index. Station has " + s.getInputs().size() + " input(s)."));
                                        return 0;
                                    }
                                    s.removeInput(index);
                                    manager.save();
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "§aInput #" + index + " removed from station '" + name + "'."));
                                    return 1;
                                })));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> setOutput(StationManager manager) {
        return literal("set-output")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : manager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            StationConfig s = manager.getByName(name);
                            if (s == null) {
                                ctx.getSource().sendError(Text.literal("§cStation not found."));
                                return 0;
                            }
                            BlockPos lookedAt = getLookedAtBlock();
                            if (lookedAt == null) {
                                ctx.getSource().sendError(Text.literal("§cLook at a container to set as output."));
                                return 0;
                            }
                            s.setOutput(lookedAt);
                            manager.save();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§aOutput container set to " + fmtPos(lookedAt) + " for station '" + name + "'."));
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> highlight(StationManager manager) {
        return literal("highlight")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : manager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            StationConfig s = manager.getByName(name);
                            if (s == null) {
                                ctx.getSource().sendError(Text.literal("§cStation not found."));
                                return 0;
                            }
                            var highlighter = AutoCraftMod.getInstance().getStationHighlighter();
                            if (highlighter.isHighlighted() &&
                                    highlighter.getHighlightedStation() == s) {
                                highlighter.clear();
                                ctx.getSource().sendFeedback(Text.literal("§aHighlighting disabled for '" + name + "'."));
                            } else {
                                highlighter.highlight(s);
                                ctx.getSource().sendFeedback(Text.literal("§aHighlighting enabled for '" + name + "'."));
                            }
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> bindRecipe(
            StationManager stationManager, RecipeManager recipeManager) {
        return literal("bind-recipe")
                .then(argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StationConfig s : stationManager.getAll()) builder.suggest(s.getName());
                            return builder.buildFuture();
                        })
                        .then(argument("recipe", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (CraftingRecipe r : recipeManager.getAll()) builder.suggest(r.getName());
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    String recipe = StringArgumentType.getString(ctx, "recipe");
                                    StationConfig s = stationManager.getByName(name);
                                    if (s == null) {
                                        ctx.getSource().sendError(Text.literal("§cStation not found."));
                                        return 0;
                                    }
                                    if (recipeManager.getByName(recipe) == null) {
                                        ctx.getSource().sendError(Text.literal("§cRecipe not found."));
                                        return 0;
                                    }
                                    s.setBoundRecipeName(recipe);
                                    stationManager.save();
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "§aStation '" + name + "' bound to recipe '" + recipe + "'."));
                                    return 1;
                                })));
    }

    // --- Helpers ---

    private static BlockPos getLookedAtBlock() {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player == null || client.crosshairTarget == null) return null;
        if (client.crosshairTarget instanceof BlockHitResult blockHit &&
                blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    private static String fmtPos(BlockPos pos) {
        if (pos == null) return "§cnot set";
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
