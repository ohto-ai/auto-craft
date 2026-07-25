package com.autocraft;

import com.autocraft.config.ModConfig;
import com.autocraft.recipe.RecipeManager;
import com.autocraft.station.StationManager;
import com.autocraft.station.StationHighlighter;
import com.autocraft.task.TaskExecutor;
import com.autocraft.command.AutoCraftCommand;
import com.autocraft.navigation.BaritoneNavigator;
import com.autocraft.navigation.NavigationProvider;
import com.autocraft.navigation.SimpleNavigator;
import com.autocraft.overlay.CraftingOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class AutoCraftMod implements ClientModInitializer {

    public static final String MOD_ID = "auto-craft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AutoCraftMod instance;

    private ModConfig config;
    private RecipeManager recipeManager;
    private StationManager stationManager;
    private StationHighlighter stationHighlighter;
    private TaskExecutor taskExecutor;
    private NavigationProvider navigationProvider;
    private CraftingOverlay craftingOverlay;

    private boolean baritoneAvailable;

    @Override
    public void onInitializeClient() {
        instance = this;

        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        if (!configDir.toFile().exists()) {
            configDir.toFile().mkdirs();
        }

        // Init config
        this.config = ModConfig.load(configDir.resolve("config.json"));

        // Init recipe system
        this.recipeManager = new RecipeManager(configDir.resolve("recipes.json"));

        // Init station system
        this.stationManager = new StationManager(configDir.resolve("stations.json"));

        // Init station highlighter
        this.stationHighlighter = new StationHighlighter();

        // Init task executor
        this.taskExecutor = new TaskExecutor(recipeManager, stationManager);

        // Init overlay
        this.craftingOverlay = new CraftingOverlay(taskExecutor);

        // Check Baritone availability and init navigation
        this.baritoneAvailable = FabricLoader.getInstance().isModLoaded("baritone");
        if (baritoneAvailable) {
            this.navigationProvider = new BaritoneNavigator();
        } else {
            this.navigationProvider = new SimpleNavigator();
        }

        // Register event callbacks
        registerEvents();

        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            AutoCraftCommand.register(dispatcher, registryAccess, recipeManager, stationManager, taskExecutor);
        });

        LOGGER.info("AutoCraft initialized! Baritone available: {}", baritoneAvailable);
    }

    private void registerEvents() {
        // Tick the task executor every client tick
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                taskExecutor.tick();
            }
        });

        // Render block highlights in the world
        WorldRenderEvents.LAST.register(context -> {
            stationHighlighter.render(context);
        });

        // Render HUD overlay
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            craftingOverlay.render(drawContext);
        });
    }

    // --- Singleton accessors ---

    public static AutoCraftMod getInstance() {
        return instance;
    }

    public ModConfig getConfig() {
        return config;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public StationManager getStationManager() {
        return stationManager;
    }

    public StationHighlighter getStationHighlighter() {
        return stationHighlighter;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public CraftingOverlay getCraftingOverlay() {
        return craftingOverlay;
    }

    public boolean isBaritoneAvailable() {
        return baritoneAvailable;
    }

    public NavigationProvider getNavigationProvider() {
        return navigationProvider;
    }
}
