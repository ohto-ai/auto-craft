package com.autocraft.station;

import com.google.gson.annotations.Expose;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a crafting station configuration:
 * - One workbench (crafting table) position
 * - Multiple input container positions (unified material pool)
 * - One output container position
 * - An optional bound recipe name
 */
public class StationConfig {

    @Expose private String name;
    @Expose private BlockPos workbench;
    @Expose private final List<BlockPos> inputs = new ArrayList<>();
    @Expose private BlockPos output;
    @Expose private String boundRecipeName;

    public StationConfig() {}

    public StationConfig(String name) {
        this.name = name;
    }

    // --- Getters / Setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BlockPos getWorkbench() {
        return workbench;
    }

    public void setWorkbench(BlockPos workbench) {
        this.workbench = workbench;
    }

    public List<BlockPos> getInputs() {
        return inputs;
    }

    public void addInput(BlockPos pos) {
        if (!inputs.contains(pos)) {
            inputs.add(pos);
        }
    }

    public void removeInput(int index) {
        if (index >= 0 && index < inputs.size()) {
            inputs.remove(index);
        }
    }

    public void removeInput(BlockPos pos) {
        inputs.remove(pos);
    }

    public BlockPos getOutput() {
        return output;
    }

    public void setOutput(BlockPos output) {
        this.output = output;
    }

    public String getBoundRecipeName() {
        return boundRecipeName;
    }

    public void setBoundRecipeName(String boundRecipeName) {
        this.boundRecipeName = boundRecipeName;
    }

    /**
     * Check if this station has all required components configured.
     */
    public boolean isComplete() {
        return name != null && !name.isBlank()
                && workbench != null
                && !inputs.isEmpty()
                && output != null
                && boundRecipeName != null && !boundRecipeName.isBlank();
    }

    /**
     * Get validation errors as a human-readable string, or null if valid.
     */
    public String getValidationError() {
        if (name == null || name.isBlank()) return "Missing station name";
        if (workbench == null) return "Workbench not set";
        if (inputs.isEmpty()) return "No input containers configured";
        if (output == null) return "Output container not set";
        if (boundRecipeName == null || boundRecipeName.isBlank()) return "No recipe bound";
        return null;
    }

    @Override
    public String toString() {
        return "StationConfig{name='" + name + "', workbench=" + workbench +
                ", inputs=" + inputs.size() + ", output=" + output +
                ", recipe='" + boundRecipeName + "'}";
    }
}
