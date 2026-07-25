package com.autocraft.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * Shared command utilities.
 */
public final class CommandHelper {

    private CommandHelper() {}

    /**
     * Suggest a name. If it contains non-ASCII chars or spaces, wrap in quotes.
     */
    public static String suggestName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c > 127 || c == ' ') return '"' + name + '"';
        }
        return name;
    }
}
