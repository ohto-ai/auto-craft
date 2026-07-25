package com.autocraft.station;

import com.autocraft.AutoCraftMod;
import com.autocraft.util.JsonUtil;
import com.google.gson.annotations.Expose;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages crafting station configurations. CRUD backed by JSON file.
 */
public class StationManager {

    @Expose private final List<StationConfig> stations = new ArrayList<>();
    private final Path storagePath;

    public StationManager(Path storagePath) {
        this.storagePath = storagePath;
        load();
    }

    // --- CRUD ---

    public boolean create(String name) {
        if (getByName(name) != null) return false;
        StationConfig config = new StationConfig(name);
        stations.add(config);
        persist();
        AutoCraftMod.LOGGER.info("Created station: {}", name);
        return true;
    }

    public boolean delete(String name) {
        StationConfig config = getByName(name);
        if (config == null) return false;
        stations.remove(config);
        persist();
        AutoCraftMod.LOGGER.info("Deleted station: {}", name);
        return true;
    }

    public StationConfig getByName(String name) {
        return stations.stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<StationConfig> getAll() {
        return Collections.unmodifiableList(stations);
    }

    /**
     * Persist changes to disk after any mutation.
     */
    public void save() {
        persist();
    }

    // --- Persistence ---

    private void load() {
        StationListWrapper wrapper = JsonUtil.readJson(storagePath, StationListWrapper.class);
        if (wrapper != null && wrapper.stations != null) {
            stations.clear();
            stations.addAll(wrapper.stations);
            AutoCraftMod.LOGGER.info("Loaded {} stations from {}", stations.size(), storagePath);
        }
    }

    private void persist() {
        StationListWrapper wrapper = new StationListWrapper();
        wrapper.stations = this.stations;
        JsonUtil.writeJson(storagePath, wrapper);
    }

    private static class StationListWrapper {
        @Expose List<StationConfig> stations;
    }
}
