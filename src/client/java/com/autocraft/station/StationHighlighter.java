package com.autocraft.station;

import com.autocraft.util.BlockHighlightRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

/**
 * Manages block highlighting for station containers.
 * Bridges StationConfig positions to the BlockHighlightRenderer.
 */
public class StationHighlighter {

    private final BlockHighlightRenderer renderer = new BlockHighlightRenderer();
    private StationConfig highlightedStation;

    /**
     * Enable highlighting for all containers in a station config.
     */
    public void highlight(StationConfig station) {
        clear();
        this.highlightedStation = station;

        if (station.getWorkbench() != null) {
            renderer.setHighlight(station.getWorkbench(), BlockHighlightRenderer.HighlightColor.WORKBENCH);
        }
        for (var input : station.getInputs()) {
            renderer.setHighlight(input, BlockHighlightRenderer.HighlightColor.INPUT);
        }
        if (station.getOutput() != null) {
            renderer.setHighlight(station.getOutput(), BlockHighlightRenderer.HighlightColor.OUTPUT);
        }
    }

    /**
     * Clear all active highlights.
     */
    public void clear() {
        renderer.clearAll();
        highlightedStation = null;
    }

    public boolean isHighlighted() {
        return highlightedStation != null;
    }

    public StationConfig getHighlightedStation() {
        return highlightedStation;
    }

    /**
     * Render call forwarded from WorldRenderEvents.LAST.
     */
    public void render(WorldRenderContext context) {
        renderer.render(context);
    }
}
