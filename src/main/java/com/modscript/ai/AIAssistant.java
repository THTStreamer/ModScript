package com.modscript.ai;

import java.util.*;

public class AIAssistant {
    private static final Map<String, String> commonErrors = new HashMap<>();
    private static final Map<String, String> codeTemplates = new HashMap<>();
    private static final List<String> tutorials = new ArrayList<>();

    static {
        commonErrors.put("Missing name in quotes", "Every create statement needs a quoted name:\n  create item \"Dragon Sword\"");
        commonErrors.put("Missing colon after 'when'", "Event handlers must end with a colon:\n  when player attacks zombie:");
        commonErrors.put("Unknown event", "Valid events: attack, break, join, click, place, hurt, die, sneak, jump, swim, craft, eat");
        commonErrors.put("Duplicate name", "Each item/block/mob must have a unique name");
        commonErrors.put("Script exceeds maximum length", "Break your script into smaller files or use includes");
        commonErrors.put("Max recursion depth exceeded", "Check for infinite loops in your event handlers");

        codeTemplates.put("sword", "create item \"Iron Sword\"\ndamage: 8\ndurability: 250\nspeed: 1.6");
        codeTemplates.put("pickaxe", "create item \"Diamond Pickaxe\"\ndamage: 5\ndurability: 1561\nmining: 3");
        codeTemplates.put("armor", "create item \"Iron Chestplate\"\ndefense: 6\ndurability: 241");
        codeTemplates.put("potion", "create item \"Healing Potion\"\nheal: 10\nstackable: true");
        codeTemplates.put("mob", "create mob \"Custom Zombie\"\nhealth: 40\nattack: 10\nspeed: 0.3\nbase: zombie");
        codeTemplates.put("boss", "create mob \"Dragon Lord\"\nhealth: 500\nattack: 50\nspeed: 0.5\nbase: ender_dragon");
        codeTemplates.put("effect", "create effect \"Poison Cloud\"\ntype: poison\nduration: 30\nlevel: 2");
        codeTemplates.put("recipe", "create recipe \"Iron Sword\"\npattern: \" I | I | S \"\nresult: \"Iron Sword\"");
        codeTemplates.put("fire_event", "when player attacks zombie:\n    set on fire for 5 seconds\n    deal 20 damage");
        codeTemplates.put("heal_event", "when player joins:\n    heal 10");
        codeTemplates.put("teleport_event", "when player clicks diamond:\n    teleport 0 64 0");
        codeTemplates.put("spawn_event", "when player breaks stone:\n    spawn \"Skeleton\"\n    play \"entity.skeleton.ambient\"");

        tutorials.add("=== ModScript Tutorial ===\n\nModScript lets you create items, blocks, mobs, effects, and recipes using simple English-like syntax.\n\nBasic Item:\n  create item \"My Sword\"\n  damage: 10\n  durability: 500\n\nBasic Block:\n  create block \"Magic Block\"\n  hardness: 5\n\nBasic Mob:\n  create mob \"Strong Zombie\"\n  health: 50\n  attack: 15\n  base: zombie\n\nEvents:\n  when player attacks zombie:\n      deal 10 damage\n      set on fire for 3 seconds\n\nActions:\n  give 1 \"My Sword\"\n  teleport 0 64 0\n  spawn \"Dragon\"\n  heal 5\n  apply \"speed\" for 30 seconds\n  play \"entity.experience_orb.pickup\"\n  shoot\n  remove all");

        tutorials.add("=== Events Reference ===\n\nplayer attacks [entity] - When player hits something\nplayer breaks [block] - When player breaks a block\nplayer joins - When player logs in\nplayer clicks [item] - When player right-clicks\nplayer places [block] - When a block is placed\nplayer hurts - When player takes damage\nplayer dies - When player dies\nplayer sneaks - When player shifts\nplayer jumps - When player jumps\nplayer swims - When player swims in water\nplayer crafts [item] - When player crafts something\nevery [number] seconds - Timed events");

        tutorials.add("=== Actions Reference ===\n\n[quantity] \"item\" - Give item to player\nteleport [x] [y] [z] - Move player\nspawn \"mob\" - Create entity nearby\nremove all - Remove nearby entities\napply \"effect\" for [seconds] - Add potion effect\nheal [amount] - Restore health\nshoot - Fire arrow\nplay \"sound\" - Play sound effect\ndeal [damage] - Damage nearby entities\nset on fire for [seconds] - Ignite player");
    }

    public static String suggestFix(String error) {
        for (var entry : commonErrors.entrySet()) {
            if (error.contains(entry.getKey())) return entry.getValue();
        }
        if (error.contains("Line") && error.contains("Col")) {
            return "Check the line and column mentioned in the error for syntax issues.";
        }
        return "Check your syntax. Type 'help' for documentation.";
    }

    public static String generateCode(String description) {
        String desc = description.toLowerCase().trim();
        for (var entry : codeTemplates.entrySet()) {
            if (desc.contains(entry.getKey())) return entry.getValue();
        }
        if (desc.contains("sword") || desc.contains("weapon")) return codeTemplates.get("sword");
        if (desc.contains("pickaxe") || desc.contains("tool")) return codeTemplates.get("pickaxe");
        if (desc.contains("armor") || desc.contains("chest")) return codeTemplates.get("armor");
        if (desc.contains("potion") || desc.contains("heal")) return codeTemplates.get("potion");
        if (desc.contains("mob") || desc.contains("monster") || desc.contains("enemy")) return codeTemplates.get("mob");
        if (desc.contains("boss") || desc.contains("strong")) return codeTemplates.get("boss");
        if (desc.contains("effect") || desc.contains("buff")) return codeTemplates.get("effect");
        if (desc.contains("recipe") || desc.contains("craft")) return codeTemplates.get("recipe");
        if (desc.contains("fire") || desc.contains("burn")) return codeTemplates.get("fire_event");
        if (desc.contains("heal") || desc.contains("health") || desc.contains("join")) return codeTemplates.get("heal_event");
        if (desc.contains("teleport") || desc.contains("warp")) return codeTemplates.get("teleport_event");
        if (desc.contains("spawn") || desc.contains("summon")) return codeTemplates.get("spawn_event");
        return "create item \"" + description + "\"\ndamage: 10\ndurability: 500";
    }

    public static String explainScript(String script) {
        StringBuilder sb = new StringBuilder();
        String[] lines = script.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("create")) sb.append(parseExplainCreate(trimmed));
            else if (trimmed.startsWith("when")) sb.append(parseExplainWhen(trimmed));
            else if (trimmed.contains(":") && !trimmed.startsWith("//")) sb.append(parseExplainProperty(trimmed));
            else sb.append("  " + trimmed + "\n");
        }
        return sb.toString();
    }

    private static String parseExplainCreate(String line) {
        String[] parts = line.split("\\s+", 3);
        if (parts.length < 3) return "  Create statement (incomplete)\n";
        String type = parts[1];
        String name = parts.length > 2 ? parts[2].replace("\"", "") : "unnamed";
        return switch (type) {
            case "item" -> "  Creates a new item called \"" + name + "\"\n";
            case "block" -> "  Creates a new block called \"" + name + "\"\n";
            case "mob" -> "  Creates a new mob entity called \"" + name + "\"\n";
            case "effect" -> "  Creates a new potion effect called \"" + name + "\"\n";
            case "recipe" -> "  Creates a crafting recipe called \"" + name + "\"\n";
            case "ability" -> "  Creates a new ability called \"" + name + "\"\n";
            default -> "  Creates a " + type + " called \"" + name + "\"\n";
        };
    }

    private static String parseExplainWhen(String line) {
        String event = line.replace("when", "").replace(":", "").trim();
        return "  Event handler: triggers when " + event + "\n";
    }

    private static String parseExplainProperty(String line) {
        String[] parts = line.split(":", 2);
        if (parts.length < 2) return "  " + line + "\n";
        String key = parts[0].trim();
        String val = parts[1].trim();
        return switch (key) {
            case "damage" -> "  Sets attack damage to " + val + " hearts\n";
            case "durability" -> "  Sets item durability to " + val + " uses\n";
            case "speed" -> "  Sets attack speed multiplier\n";
            case "health" -> "  Sets mob health to " + val + " hearts\n";
            case "attack" -> "  Sets mob attack damage to " + val + " hearts\n";
            case "base" -> "  Based on vanilla mob: " + val + "\n";
            case "heal" -> "  Heals player for " + val + " hearts\n";
            default -> "  " + key + " = " + val + "\n";
        };
    }

    public static List<String> getTutorial() { return tutorials; }
    public static String getTutorial(int index) { return index >= 0 && index < tutorials.size() ? tutorials.get(index) : tutorials.get(0); }
    public static List<String> getCodeTemplates() { return new ArrayList<>(codeTemplates.keySet()); }
    public static String getTemplate(String name) { return codeTemplates.getOrDefault(name, null); }
}
