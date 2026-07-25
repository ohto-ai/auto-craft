package com.autocraft.command;

import static com.autocraft.command.CommandHelper.suggestName;
import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.recipe.RecipeCapture;
import com.autocraft.recipe.RecipeManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.Text;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Subcommands for recipe management: /autocraft recipe ...
 */
public final class RecipeSubCommands {

    private RecipeSubCommands() {}

    public static LiteralArgumentBuilder<FabricClientCommandSource> save(RecipeManager manager) {
        return literal("save")
                .then(argument("name", StringArgumentType.string())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            // Capture recipe from current crafting screen
                            var client = MinecraftClient.getInstance();
                            if (!(client.currentScreen instanceof CraftingScreen screen)) {
                                ctx.getSource().sendError(Text.literal("§cOpen a crafting table first."));
                                return 0;
                            }
                            CraftingScreenHandler handler = screen.getScreenHandler();
                            CraftingRecipe recipe = RecipeCapture.capture(handler);
                            if (recipe == null) {
                                ctx.getSource().sendError(Text.literal("§cNo items in crafting grid."));
                                return 0;
                            }
                            recipe.setName(name);
                            if (manager.getByName(name) != null) {
                                manager.overwrite(recipe);
                                ctx.getSource().sendFeedback(Text.literal("§aRecipe '" + name + "' overwritten."));
                            } else {
                                manager.save(recipe);
                                ctx.getSource().sendFeedback(Text.literal("§aRecipe '" + name + "' saved."));
                            }
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> list(RecipeManager manager) {
        return literal("list").executes(ctx -> {
            List<CraftingRecipe> recipes = manager.getAll();
            if (recipes.isEmpty()) {
                ctx.getSource().sendFeedback(Text.literal("§7No saved recipes."));
                return 0;
            }
            ctx.getSource().sendFeedback(Text.literal("§6--- Saved Recipes (" + recipes.size() + ") ---"));
            for (CraftingRecipe recipe : recipes) {
                String resultName = recipe.getResult() != null ? recipe.getResult().itemId : "none";
                int resultCount = recipe.getResult() != null ? recipe.getResult().count : 0;
                ctx.getSource().sendFeedback(Text.literal(
                        "§e  " + recipe.getName() + " §7→ " + resultName + " x" + resultCount));
            }
            return 1;
        });
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> show(RecipeManager manager) {
        return literal("show")
                .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            for (CraftingRecipe r : manager.getAll()) {
                                builder.suggest(suggestName(r.getName()));
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            CraftingRecipe recipe = manager.getByName(name);
                            if (recipe == null) {
                                ctx.getSource().sendError(Text.literal("§cRecipe '" + name + "' not found."));
                                return 0;
                            }
                            String resultStr = recipe.getResult().itemId + " x" + recipe.getResult().count;
                            int ingCount = recipe.getFilledSlots().size();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§6Recipe: §e" + recipe.getName() +
                                            " §7| Result: §f" + resultStr +
                                            " §7| Ingredients: §f" + ingCount + " types"));
                            for (var entry : recipe.getFilledSlots().entrySet()) {
                                int slot = entry.getKey();
                                CraftingRecipe.GridEntry grid = entry.getValue();
                                int row = slot / 3;
                                int col = slot % 3;
                                ctx.getSource().sendFeedback(Text.literal(
                                        "§7  [" + row + "," + col + "] §f" + grid.itemId + " x" + grid.count));
                            }
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> delete(RecipeManager manager) {
        return literal("delete")
                .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            for (CraftingRecipe r : manager.getAll()) {
                                builder.suggest(suggestName(r.getName()));
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            if (manager.delete(name)) {
                                ctx.getSource().sendFeedback(Text.literal("§aRecipe '" + name + "' deleted."));
                            } else {
                                ctx.getSource().sendError(Text.literal("§cRecipe '" + name + "' not found."));
                            }
                            return 1;
                        }));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> overwrite(RecipeManager manager) {
        return literal("overwrite")
                .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            for (CraftingRecipe r : manager.getAll()) builder.suggest(suggestName(r.getName()));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            var client = MinecraftClient.getInstance();
                            if (!(client.currentScreen instanceof CraftingScreen screen)) {
                                ctx.getSource().sendError(Text.literal("§cOpen a crafting table first."));
                                return 0;
                            }
                            CraftingRecipe recipe = RecipeCapture.capture(screen.getScreenHandler());
                            if (recipe == null) {
                                ctx.getSource().sendError(Text.literal("§cNo items in crafting grid."));
                                return 0;
                            }
                            recipe.setName(name);
                            manager.overwrite(recipe);
                            ctx.getSource().sendFeedback(Text.literal("§aRecipe '" + name + "' overwritten."));
                            return 1;
                        }));
    }
}
