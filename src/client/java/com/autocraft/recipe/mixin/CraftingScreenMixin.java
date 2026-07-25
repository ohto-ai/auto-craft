package com.autocraft.recipe.mixin;

import com.autocraft.recipe.CraftingRecipe;
import com.autocraft.recipe.RecipeCapture;
import com.autocraft.recipe.SaveRecipeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends HandledScreen<CraftingScreenHandler> {

    @Unique private ButtonWidget saveRecipeButton;

    private CraftingScreenMixin(CraftingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int bx = this.x + 150;
        int by = this.y + 30;
        saveRecipeButton = ButtonWidget.builder(
                Text.literal("§a💾 +"),
                btn -> {
                    CraftingRecipe recipe = RecipeCapture.capture(this.getScreenHandler());
                    if (recipe == null) {
                        if (MinecraftClient.getInstance().player != null)
                            MinecraftClient.getInstance().player.sendMessage(
                                    Text.literal("§cNo items in crafting grid to save."), false);
                        return;
                    }
                    MinecraftClient.getInstance().setScreen(
                            new SaveRecipeScreen(recipe, MinecraftClient.getInstance().currentScreen));
                }
        ).dimensions(bx, by, 22, 20).build();
        this.addDrawableChild(saveRecipeButton);
    }
}
