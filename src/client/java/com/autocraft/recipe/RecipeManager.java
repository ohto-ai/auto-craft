package com.autocraft.recipe;

import com.autocraft.AutoCraftMod;
import com.autocraft.util.JsonUtil;
import com.google.gson.annotations.Expose;

import java.nio.file.Path;
import java.util.*;

/**
 * Manages saved crafting recipes. Provides CRUD operations backed by a JSON file.
 * Thread-safe for single-threaded client use.
 */
public class RecipeManager {

    @Expose private final List<CraftingRecipe> recipes = new ArrayList<>();
    private final Path storagePath;

    public RecipeManager(Path storagePath) {
        this.storagePath = storagePath;
        load();
    }

    // --- CRUD ---

    /**
     * Save a new recipe. Returns false if a recipe with this name already exists.
     */
    public boolean save(CraftingRecipe recipe) {
        if (recipe == null || recipe.getName() == null || recipe.getName().isBlank()) {
            return false;
        }
        if (getByName(recipe.getName()) != null) {
            return false; // duplicate name
        }
        recipes.add(recipe);
        persist();
        AutoCraftMod.LOGGER.info("Saved recipe: {}", recipe.getName());
        return true;
    }

    /**
     * Overwrite an existing recipe with the same name, or save if new.
     */
    public void overwrite(CraftingRecipe recipe) {
        CraftingRecipe existing = getByName(recipe.getName());
        if (existing != null) {
            recipes.remove(existing);
        }
        recipes.add(recipe);
        persist();
        AutoCraftMod.LOGGER.info("Overwritten recipe: {}", recipe.getName());
    }

    /**
     * Delete a recipe by name. Returns false if not found.
     */
    public boolean delete(String name) {
        CraftingRecipe recipe = getByName(name);
        if (recipe == null) return false;
        recipes.remove(recipe);
        persist();
        AutoCraftMod.LOGGER.info("Deleted recipe: {}", name);
        return true;
    }

    public CraftingRecipe getByName(String name) {
        return recipes.stream()
                .filter(r -> r.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<CraftingRecipe> getAll() {
        return Collections.unmodifiableList(recipes);
    }

    public int getCount() {
        return recipes.size();
    }

    // --- Persistence ---

    private void load() {
        RecipeListWrapper wrapper = JsonUtil.readJson(storagePath, RecipeListWrapper.class);
        if (wrapper != null && wrapper.recipes != null) {
            recipes.clear();
            recipes.addAll(wrapper.recipes);
            AutoCraftMod.LOGGER.info("Loaded {} recipes from {}", recipes.size(), storagePath);
        }
    }

    private void persist() {
        RecipeListWrapper wrapper = new RecipeListWrapper();
        wrapper.recipes = this.recipes;
        JsonUtil.writeJson(storagePath, wrapper);
    }

    /**
     * Wrapper for JSON array serialization (Gson doesn't serialize top-level arrays nicely).
     */
    private static class RecipeListWrapper {
        @Expose List<CraftingRecipe> recipes;
    }
}
