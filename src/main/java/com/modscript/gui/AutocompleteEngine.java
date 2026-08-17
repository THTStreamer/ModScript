package com.modscript.gui;

import java.util.ArrayList;
import java.util.List;

public class AutocompleteEngine {
    private static final List<String> KEYWORDS = List.of(
        "create", "item", "block", "when", "this", "is", "hits", "set", "the", "on",
        "fire", "for", "seconds", "deal", "damage", "play", "sound", "particles", "extra",
        "if", "else", "while", "for", "each", "return", "true", "false", "yes", "no"
    );

    private static final List<String> ITEM_PROPERTIES = List.of(
        "name", "damage", "durability", "stackSize", "fireResistant", "tab"
    );

    private static final List<String> BLOCK_PROPERTIES = List.of(
        "name", "hardness", "resistance", "requiresTool", "lightLevel"
    );

    private static final List<String> EVENTS = List.of(
        "player attacks entity", "player breaks block", "player joins world",
        "player leaves world", "player takes damage", "player dies",
        "entity spawns", "entity dies", "block is placed", "block is broken",
        "item is used", "right click", "left click"
    );

    private static final List<String> ACTIONS = List.of(
        "set", "deal", "play", "create", "give", "teleport", "spawn",
        "remove", "add", "multiply", "divide"
    );

    private static final List<String> SOUNDS = List.of(
        "level_up", "dragon.roar", "lightning", "explosion", "pickup",
        "damage", "heal", "teleport", "portal", "ambient.cave"
    );

    public static List<String> getSuggestions(String currentLine, int cursorPosition) {
        List<String> suggestions = new ArrayList<>();
        String prefix = currentLine.substring(0, Math.min(cursorPosition, currentLine.length())).toLowerCase();

        if (prefix.isEmpty()) {
            suggestions.addAll(List.of("create", "when"));
            return suggestions;
        }

        // Check context
        String trimmed = prefix.trim();

        if (trimmed.equals("create")) {
            suggestions.add("item");
            suggestions.add("block");
        } else if (trimmed.startsWith("create ") && !trimmed.contains("\"")) {
            suggestions.add("\"My Item\"");
        } else if (trimmed.endsWith(":") || trimmed.endsWith("damage:") ||
                   trimmed.endsWith("durability:") || trimmed.endsWith("hardness:") ||
                   trimmed.endsWith("resistance:") || trimmed.endsWith("lightLevel:")) {
            suggestions.add("10");
            suggestions.add("100");
            suggestions.add("1000");
        } else if (trimmed.startsWith("when ")) {
            for (String event : EVENTS) {
                if (event.startsWith(trimmed.substring(5).toLowerCase())) {
                    suggestions.add(event);
                }
            }
            if (suggestions.isEmpty()) {
                suggestions.addAll(EVENTS);
            }
        } else if (trimmed.startsWith("set ")) {
            suggestions.add("the");
        } else if (trimmed.contains("on fire")) {
            suggestions.add("for 5 seconds");
            suggestions.add("for 10 seconds");
        } else if (trimmed.contains("deal")) {
            suggestions.add("damage");
        } else if (trimmed.contains("play")) {
            suggestions.add("sound");
        } else if (trimmed.startsWith("play sound")) {
            for (String sound : SOUNDS) {
                suggestions.add("\"" + sound + "\"");
            }
        } else {
            // General keyword matching
            for (String keyword : KEYWORDS) {
                if (keyword.startsWith(trimmed)) {
                    suggestions.add(keyword);
                }
            }
            for (String event : EVENTS) {
                if (event.startsWith(trimmed)) {
                    suggestions.add(event);
                }
            }
        }

        return suggestions;
    }

    public static String getContextHint(String currentLine) {
        String trimmed = currentLine.trim().toLowerCase();

        if (trimmed.startsWith("create item")) {
            return "Properties: name, damage, durability, stackSize, fireResistant";
        } else if (trimmed.startsWith("create block")) {
            return "Properties: name, hardness, resistance, requiresTool, lightLevel";
        } else if (trimmed.startsWith("when")) {
            return "Events: player attacks, player breaks, player joins, etc.";
        } else if (trimmed.contains("damage:")) {
            return "Enter a number (e.g., 10, 25, 100)";
        } else if (trimmed.contains("durability:")) {
            return "Enter a number (e.g., 100, 500, 1000)";
        } else if (trimmed.contains("hardness:")) {
            return "Enter a number (e.g., 1.0, 5.0, 10.0)";
        }

        return null;
    }

    public static List<String> getDocumentation(String keyword) {
        return switch (keyword.toLowerCase()) {
            case "create" -> List.of(
                "CREATE - Creates a new item or block",
                "Syntax: create item \"Name\"",
                "Syntax: create block \"Name\"",
                "Example: create item \"Dragon Sword\""
            );
            case "when" -> List.of(
                "WHEN - Registers an event handler",
                "Syntax: when <event>:",
                "Example: when player attacks zombie:"
            );
            case "damage" -> List.of(
                "DAMAGE - Sets item attack damage",
                "Syntax: damage: <number>",
                "Example: damage: 25"
            );
            case "durability" -> List.of(
                "DURABILITY - Sets item durability",
                "Syntax: durability: <number>",
                "Example: durability: 1500"
            );
            case "hardness" -> List.of(
                "HARDNESS - Sets block hardness",
                "Syntax: hardness: <number>",
                "Example: hardness: 5.0"
            );
            default -> List.of("No documentation available for: " + keyword);
        };
    }
}
