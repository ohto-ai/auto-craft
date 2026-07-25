package com.autocraft.recipe;

import com.autocraft.AutoCraftMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Popup screen shown after clicking the "save recipe" button in the crafting table.
 * Displays an editable text field pre-filled with the result item's i18n name.
 * Enter or click Save to confirm, Escape to cancel.
 */
public class SaveRecipeScreen extends Screen {

    private final CraftingRecipe capturedRecipe;
    private TextFieldWidget nameField;
    private final Screen parent;

    public SaveRecipeScreen(CraftingRecipe recipe, Screen parent) {
        super(Text.literal("Save Recipe"));
        this.capturedRecipe = recipe;
        this.parent = parent;
    }

    @Override
    protected void init() {
        TextRenderer font = textRenderer;
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Text field — pre-fill with result item's display name
        String defaultName = getDefaultName();
        nameField = new TextFieldWidget(font, cx - 100, cy - 10, 200, 20, Text.literal(""));
        nameField.setText(defaultName);
        nameField.setCursorToStart(true);
        nameField.setSelectionStart(0);
        nameField.setSelectionEnd(defaultName.length());
        addSelectableChild(nameField);
        setInitialFocus(nameField);

        // Save button
        addDrawableChild(ButtonWidget.builder(Text.literal("§aSave"), btn -> save())
                .dimensions(cx - 105, cy + 20, 100, 20).build());

        // Cancel button
        addDrawableChild(ButtonWidget.builder(Text.literal("§cCancel"), btn -> close())
                .dimensions(cx + 5, cy + 20, 100, 20).build());
    }

    /** Get the result item's translated display name as default. */
    private String getDefaultName() {
        var stack = capturedRecipe.createResultStack();
        if (!stack.isEmpty()) {
            return stack.getName().getString();
        }
        // Fallback: use raw item ID stripped of namespace
        String id = capturedRecipe.getResult().itemId;
        if (id != null && id.contains(":")) {
            return id.substring(id.indexOf(':') + 1);
        }
        return "recipe";
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;

        capturedRecipe.setName(name);
        var manager = AutoCraftMod.getInstance().getRecipeManager();
        if (manager.getByName(name) != null) {
            // Already exists — overwrite
            manager.overwrite(capturedRecipe);
        } else {
            manager.save(capturedRecipe);
        }
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§aRecipe '" + name + "' saved."), false);
        }
        close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim background
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        TextRenderer font = textRenderer;
        int cx = this.width / 2;
        int cy = this.height / 2;
        context.drawCenteredTextWithShadow(font, "Save Recipe", cx, cy - 30, 0xFFFFFF);
        nameField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent); // back to crafting table
    }
}
