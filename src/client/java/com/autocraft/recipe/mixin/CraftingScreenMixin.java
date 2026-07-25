package com.autocraft.recipe.mixin;

import com.autocraft.AutoCraftMod;
import com.autocraft.recipe.CraftingRecipe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "Save Recipe" button to the vanilla crafting table GUI.
 * The button captures the current 3×3 grid + result and saves it.
 */
@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends HandledScreen<CraftingScreenHandler> {

    @Unique
    private ButtonWidget saveRecipeButton;

    // Constructor required by HandledScreen superclass
    private CraftingScreenMixin(CraftingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Position the save button below the crafting result slot (typically near x=124, y=35)
        int buttonX = this.x + 150;
        int buttonY = this.y + 30;

        saveRecipeButton = ButtonWidget.builder(
                Text.literal("§a💾 +"),
                button -> onSaveRecipe()
        ).dimensions(buttonX, buttonY, 22, 20).build();

        this.addDrawableChild(saveRecipeButton);
    }

    @Unique
    private void onSaveRecipe() {
        CraftingScreenHandler handler = this.getScreenHandler();

        // Build recipe from the current crafting grid
        CraftingRecipe recipe = new CraftingRecipe();

        // Slots layout in CraftingScreenHandler:
        //   slot 0 = result
        //   slots 1-9 = 3×3 crafting grid (1=top-left, 2=top-middle, ..., 9=bottom-right)
        boolean hasAnyItem = false;
        for (int i = 0; i < 9; i++) {
            Slot slot = handler.getSlot(i + 1); // slots are 0-indexed, grid starts at index 1
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                recipe.setSlot(i, itemId, stack.getCount());
                hasAnyItem = true;
            }
        }

        if (!hasAnyItem) {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§cNo items in crafting grid to save."), false);
            return;
        }

        // Capture result
        Slot resultSlot = handler.getSlot(0);
        ItemStack resultStack = resultSlot.getStack();
        if (!resultStack.isEmpty()) {
            String resultId = Registries.ITEM.getId(resultStack.getItem()).toString();
            recipe.setResult(resultId, resultStack.getCount());
        }

        // Open a prompt: since we can't easily render a text-field in this mixin,
        // send a prompt to chat asking the player to use the /autocraft recipe save command.
        // For a more polished UX, we pre-populate and auto-save with a timestamp name;
        // the player can rename via command.

        String autoName = "recipe_" + System.currentTimeMillis() % 100000;
        recipe.setName(autoName);

        var recipeManager = AutoCraftMod.getInstance().getRecipeManager();
        if (recipeManager.save(recipe)) {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§aRecipe saved as '" + autoName + "'. " +
                            "Use §e/autocraft recipe list§a to view, " +
                            "§e/autocraft recipe overwrite <name>§a to rename."), false);
        } else {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§cFailed to save recipe. Name collision on '" + autoName + "'?"), false);
        }
    }
}
