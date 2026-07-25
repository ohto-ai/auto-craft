package com.autocraft.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders colored wireframe outlines on target blocks in the world.
 * Used for visualizing station container locations.
 */
public class BlockHighlightRenderer {

    public enum HighlightColor {
        INPUT(0x00FF00),
        OUTPUT(0xFFA500),
        WORKBENCH(0x00BFFF),
        DEFAULT(0xFFFFFF);

        final int color;
        HighlightColor(int color) { this.color = color; }
        public int getColor() { return color; }
    }

    private final Map<BlockPos, HighlightColor> activeHighlights = new ConcurrentHashMap<>();

    public void setHighlight(BlockPos pos, HighlightColor color) {
        activeHighlights.put(pos, color);
    }

    public void removeHighlight(BlockPos pos) {
        activeHighlights.remove(pos);
    }

    public void clearAll() {
        activeHighlights.clear();
    }

    public boolean hasHighlight(BlockPos pos) {
        return activeHighlights.containsKey(pos);
    }

    public void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (activeHighlights.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(2.0f);
        RenderSystem.disableDepthTest();

        for (Map.Entry<BlockPos, HighlightColor> entry : activeHighlights.entrySet()) {
            int color = entry.getValue().getColor();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            renderBlockOutline(buffer, matrices, entry.getKey(), r, g, b, 0.8f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    private void renderBlockOutline(BufferBuilder buffer, MatrixStack matrices, BlockPos pos,
                                     float r, float g, float b, float a) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();

        float[][] verts = {
            {x,y,z}, {x+1,y,z}, {x+1,y,z}, {x+1,y,z+1},
            {x+1,y,z+1}, {x,y,z+1}, {x,y,z+1}, {x,y,z},
            {x,y,z}, {x,y+1,z}, {x+1,y,z}, {x+1,y+1,z},
            {x+1,y,z+1}, {x+1,y+1,z+1}, {x,y,z+1}, {x,y+1,z+1},
            {x,y+1,z}, {x+1,y+1,z}, {x+1,y+1,z}, {x+1,y+1,z+1},
            {x+1,y+1,z+1}, {x,y+1,z+1}, {x,y+1,z+1}, {x,y+1,z},
        };
        for (float[] v : verts) {
            buffer.vertex(model, v[0], v[1], v[2]).color(r, g, b, a);
        }
    }
}
