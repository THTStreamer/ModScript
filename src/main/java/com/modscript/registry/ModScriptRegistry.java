package com.modscript.registry;

import com.google.gson.*;
import com.modscript.script.ASTNode;
import com.modscript.script.ASTNode.PropertyNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModScriptRegistry {
    private static final Map<String, Map<String, Object>> registeredItems = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> registeredBlocks = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> registeredMobs = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> registeredEffects = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> registeredRecipes = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> registeredAbilities = new ConcurrentHashMap<>();

    public static void registerItem(String name, List<PropertyNode> properties) throws Exception {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("type", "item");
        for (var prop : properties) {
            item.put(prop.getKey(), extractValue(prop.getValue()));
        }
        registeredItems.put(name, item);
        saveToDisk("items", name, item);
    }

    public static void registerBlock(String name, List<PropertyNode> properties) throws Exception {
        Map<String, Object> block = new HashMap<>();
        block.put("name", name);
        block.put("type", "block");
        for (var prop : properties) {
            block.put(prop.getKey(), extractValue(prop.getValue()));
        }
        registeredBlocks.put(name, block);
        saveToDisk("blocks", name, block);
    }

    public static void registerMob(String name, List<PropertyNode> properties) throws Exception {
        Map<String, Object> mob = new HashMap<>();
        mob.put("name", name);
        mob.put("type", "mob");
        for (var prop : properties) {
            mob.put(prop.getKey(), extractValue(prop.getValue()));
        }
        registeredMobs.put(name, mob);
        saveToDisk("mobs", name, mob);
    }

    public static void registerEffect(String name, List<PropertyNode> properties) throws Exception {
        Map<String, Object> effect = new HashMap<>();
        effect.put("name", name);
        effect.put("type", "effect");
        for (var prop : properties) {
            effect.put(prop.getKey(), extractValue(prop.getValue()));
        }
        registeredEffects.put(name, effect);
        saveToDisk("effects", name, effect);
    }

    public static void registerRecipe(String name, List<PropertyNode> properties) throws Exception {
        Map<String, Object> recipe = new HashMap<>();
        recipe.put("name", name);
        recipe.put("type", "recipe");
        for (var prop : properties) {
            recipe.put(prop.getKey(), extractValue(prop.getValue()));
        }
        registeredRecipes.put(name, recipe);
        saveToDisk("recipes", name, recipe);
    }

    public static void registerAbility(String name, List<PropertyNode> properties) throws Exception {
        Map<String, Object> ability = new HashMap<>();
        ability.put("name", name);
        ability.put("type", "ability");
        for (var prop : properties) {
            ability.put(prop.getKey(), extractValue(prop.getValue()));
        }
        registeredAbilities.put(name, ability);
        saveToDisk("abilities", name, ability);
    }

    private static Object extractValue(ASTNode node) {
        if (node instanceof ASTNode.StringLiteral str) return str.getValue();
        if (node instanceof ASTNode.NumberLiteral num) return num.getValue();
        if (node instanceof ASTNode.BooleanLiteral bool) return bool.isValue();
        return node.toString();
    }

    public static ItemStack getItemStack(String name) {
        Map<String, Object> item = registeredItems.get(name);
        if (item == null) return null;
        return new ItemStack(Items.STONE);
    }

    public static BlockState getBlockState(String name) {
        return Blocks.STONE.defaultBlockState();
    }

    public static EntityType<?> getMobType(String name) {
        Map<String, Object> mob = registeredMobs.get(name);
        if (mob == null) return null;
        String mobType = (String) mob.getOrDefault("base", "zombie");
        return switch (mobType) {
            case "zombie" -> EntityType.ZOMBIE;
            case "skeleton" -> EntityType.SKELETON;
            case "creeper" -> EntityType.CREEPER;
            case "spider" -> EntityType.SPIDER;
            case "enderman" -> EntityType.ENDERMAN;
            default -> EntityType.ZOMBIE;
        };
    }

    public static void applyEffect(ServerPlayer player, String effectName, int duration) {
        MobEffectInstance effect = switch (effectName.toLowerCase()) {
            case "speed" -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration * 20, 1);
            case "strength" -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration * 20, 1);
            case "regeneration" -> new MobEffectInstance(MobEffects.REGENERATION, duration * 20, 1);
            case "invisibility" -> new MobEffectInstance(MobEffects.INVISIBILITY, duration * 20, 0);
            case "fire resistance" -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration * 20, 0);
            case "healing" -> new MobEffectInstance(MobEffects.HEAL, duration * 20, 1);
            case "poison" -> new MobEffectInstance(MobEffects.POISON, duration * 20, 1);
            case "weakness" -> new MobEffectInstance(MobEffects.WEAKNESS, duration * 20, 1);
            case "slowness" -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration * 20, 1);
            case "haste" -> new MobEffectInstance(MobEffects.DIG_SPEED, duration * 20, 1);
            case "jump boost" -> new MobEffectInstance(MobEffects.JUMP, duration * 20, 1);
            case "absorption" -> new MobEffectInstance(MobEffects.ABSORPTION, duration * 20, 1);
            case "health boost" -> new MobEffectInstance(MobEffects.HEALTH_BOOST, duration * 20, 4);
            case "night vision" -> new MobEffectInstance(MobEffects.NIGHT_VISION, duration * 20, 0);
            default -> null;
        };
        if (effect != null) player.addEffect(effect);
    }

    private static void saveToDisk(String folder, String name, Map<String, Object> data) throws Exception {
        MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        Path dataFolder = null;
        if (server != null) {
            dataFolder = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("modscript/data").resolve(folder);
        } else {
            dataFolder = Paths.get("modscript_data").resolve(folder);
        }
        Files.createDirectories(dataFolder);
        Path file = dataFolder.resolve(name + ".json");
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        for (var entry : data.entrySet()) {
            if (entry.getValue() instanceof String s) json.addProperty(entry.getKey(), s);
            else if (entry.getValue() instanceof Number n) json.addProperty(entry.getKey(), n);
            else if (entry.getValue() instanceof Boolean b) json.addProperty(entry.getKey(), b);
        }
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }

    public static void loadAll() {
        try {
            MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            Path dataFolder = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("modscript/data");
            loadFolder(dataFolder.resolve("items"), registeredItems);
            loadFolder(dataFolder.resolve("blocks"), registeredBlocks);
            loadFolder(dataFolder.resolve("mobs"), registeredMobs);
            loadFolder(dataFolder.resolve("effects"), registeredEffects);
            loadFolder(dataFolder.resolve("recipes"), registeredRecipes);
            loadFolder(dataFolder.resolve("abilities"), registeredAbilities);
        } catch (Exception e) { System.err.println("ModScript: Failed to load data: " + e.getMessage()); }
    }

    private static void loadFolder(Path folder, Map<String, Map<String, Object>> target) throws Exception {
        if (!Files.exists(folder)) return;
        try (var stream = Files.list(folder)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                try {
                    String json = Files.readString(path);
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    Map<String, Object> data = new HashMap<>();
                    obj.entrySet().forEach(e -> {
                        if (e.getValue().isJsonPrimitive()) {
                            var prim = e.getValue().getAsJsonPrimitive();
                            if (prim.isString()) data.put(e.getKey(), prim.getAsString());
                            else if (prim.isNumber()) data.put(e.getKey(), prim.getAsDouble());
                            else if (prim.isBoolean()) data.put(e.getKey(), prim.getAsBoolean());
                        }
                    });
                    String name = obj.has("name") ? obj.get("name").getAsString() : path.getFileName().toString().replace(".json", "");
                    target.put(name, data);
                } catch (Exception e) { System.err.println("ModScript: Failed to load " + path + ": " + e.getMessage()); }
            });
        }
    }

    public static Map<String, Map<String, Object>> getRegisteredItems() { return registeredItems; }
    public static Map<String, Map<String, Object>> getRegisteredBlocks() { return registeredBlocks; }
    public static Map<String, Map<String, Object>> getRegisteredMobs() { return registeredMobs; }
    public static Map<String, Map<String, Object>> getRegisteredEffects() { return registeredEffects; }
    public static Map<String, Map<String, Object>> getRegisteredRecipes() { return registeredRecipes; }
    public static Map<String, Map<String, Object>> getRegisteredAbilities() { return registeredAbilities; }

    public static void init(MinecraftServer server) {
        if (server != null) {
            loadAll();
        }
    }

    public static Map<String, Map<String, Object>> loadAllItems() {
        loadAll();
        return registeredItems;
    }

    public static void reset() {
        registeredItems.clear();
        registeredBlocks.clear();
        registeredMobs.clear();
        registeredEffects.clear();
        registeredRecipes.clear();
        registeredAbilities.clear();
    }
}
