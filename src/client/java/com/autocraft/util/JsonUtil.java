package com.autocraft.util;

import com.autocraft.AutoCraftMod;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared JSON serialization / deserialization utility.
 * Includes custom adapters for Minecraft types like BlockPos.
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
            .create();

    private JsonUtil() {}

    public static <T> T readJson(Path path, Class<T> clazz) {
        if (!Files.exists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, clazz);
        } catch (IOException | JsonSyntaxException e) {
            AutoCraftMod.LOGGER.error("Failed to read JSON from {}: {}", path, e.getMessage());
            return null;
        }
    }

    public static void writeJson(Path path, Object object) {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(object, writer);
            }
        } catch (IOException e) {
            AutoCraftMod.LOGGER.error("Failed to write JSON to {}: {}", path, e.getMessage());
        }
    }

    public static Gson getGson() { return GSON; }

    /**
     * Custom adapter: BlockPos ↔ [x, y, z] integer array.
     * BlockPos has final fields, so default reflection deserialization fails.
     */
    private static class BlockPosAdapter implements JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {
        @Override
        public JsonElement serialize(BlockPos src, Type type, JsonSerializationContext ctx) {
            JsonArray arr = new JsonArray();
            arr.add(src.getX());
            arr.add(src.getY());
            arr.add(src.getZ());
            return arr;
        }

        @Override
        public BlockPos deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonArray arr = json.getAsJsonArray();
            return new BlockPos(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
        }
    }
}
