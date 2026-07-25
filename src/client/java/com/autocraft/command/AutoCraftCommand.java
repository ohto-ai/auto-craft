package com.autocraft.command;

import com.autocraft.recipe.RecipeManager;
import com.autocraft.station.StationManager;
import com.autocraft.task.TaskExecutor;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Root command registration for /autocraft.
 * Delegates subcommands to RecipeSubCommands, StationSubCommands, and TaskSubCommands.
 */
public final class AutoCraftCommand {

    private AutoCraftCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                 CommandRegistryAccess registryAccess,
                                 RecipeManager recipeManager,
                                 StationManager stationManager,
                                 TaskExecutor taskExecutor) {
        dispatcher.register(
                literal("autocraft")
                        .then(literal("recipe")
                                .then(RecipeSubCommands.save(recipeManager))
                                .then(RecipeSubCommands.list(recipeManager))
                                .then(RecipeSubCommands.show(recipeManager))
                                .then(RecipeSubCommands.delete(recipeManager))
                                .then(RecipeSubCommands.overwrite(recipeManager))
                        )
                        .then(literal("station")
                                .then(StationSubCommands.create(stationManager))
                                .then(StationSubCommands.delete(stationManager))
                                .then(StationSubCommands.list(stationManager))
                                .then(StationSubCommands.show(stationManager))
                                .then(StationSubCommands.setWorkbench(stationManager))
                                .then(StationSubCommands.addInput(stationManager))
                                .then(StationSubCommands.removeInput(stationManager))
                                .then(StationSubCommands.setOutput(stationManager))
                                .then(StationSubCommands.highlight(stationManager))
                                .then(StationSubCommands.bindRecipe(stationManager, recipeManager))
                        )
                        .then(literal("task")
                                .then(TaskSubCommands.start(taskExecutor, stationManager, recipeManager))
                                .then(TaskSubCommands.pause(taskExecutor))
                                .then(TaskSubCommands.resume(taskExecutor))
                                .then(TaskSubCommands.stop(taskExecutor))
                                .then(TaskSubCommands.status(taskExecutor))
                        )
        );
    }
}
