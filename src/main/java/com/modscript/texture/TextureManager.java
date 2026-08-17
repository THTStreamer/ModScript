package com.modscript.texture;

import net.minecraft.server.MinecraftServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TextureManager {
    private static final Map<String, TextureData> textures = new ConcurrentHashMap<>();
    private static final Map<String, String> baseTextures = new LinkedHashMap<>();
    private static final Map<String, int[]> palette = new LinkedHashMap<>();

    static {
        baseTextures.put("stone", "block/stone");
        baseTextures.put("dirt", "block/dirt");
        baseTextures.put("grass", "block/grass_block_top");
        baseTextures.put("cobblestone", "block/cobblestone");
        baseTextures.put("planks", "block/oak_planks");
        baseTextures.put("bedrock", "block/bedrock");
        baseTextures.put("sand", "block/sand");
        baseTextures.put("gravel", "block/gravel");
        baseTextures.put("gold_ore", "block/gold_ore");
        baseTextures.put("iron_ore", "block/iron_ore");
        baseTextures.put("coal_ore", "block/coal_ore");
        baseTextures.put("diamond_ore", "block/diamond_ore");
        baseTextures.put("crafting_table", "block/crafting_table");
        baseTextures.put("furnace", "block/furnace_front");
        baseTextures.put("chest", "block/chest_front");
        baseTextures.put("log", "block/oak_log");
        baseTextures.put("leaves", "block/oak_leaves");
        baseTextures.put("glass", "block/glass");
        baseTextures.put("diamond_block", "block/diamond_block");
        baseTextures.put("emerald_block", "block/emerald_block");
        baseTextures.put("iron_block", "block/iron_block");
        baseTextures.put("gold_block", "block/gold_block");
        baseTextures.put("redstone_block", "block/redstone_block");
        baseTextures.put("lapis_block", "block/lapis_block");
        baseTextures.put("netherrack", "block/netherrack");
        baseTextures.put("soul_sand", "block/soul_sand");
        baseTextures.put("glowstone", "block/glowstone");
        baseTextures.put("obsidian", "block/obsidian");
        baseTextures.put("end_stone", "block/end_stone");

        baseTextures.put("sword", "item/iron_sword");
        baseTextures.put("pickaxe", "item/iron_pickaxe");
        baseTextures.put("axe", "item/iron_axe");
        baseTextures.put("shovel", "item/iron_shovel");
        baseTextures.put("hoe", "item/iron_hoe");
        baseTextures.put("bow", "item/bow");
        baseTextures.put("crossbow", "item/crossbow");
        baseTextures.put("shield", "item/shield");
        baseTextures.put("helmet", "item/iron_helmet");
        baseTextures.put("chestplate", "item/iron_chestplate");
        baseTextures.put("leggings", "item/iron_leggings");
        baseTextures.put("boots", "item/iron_boots");
        baseTextures.put("apple", "item/apple");
        baseTextures.put("bread", "item/bread");
        baseTextures.put("beef", "item/beef");
        baseTextures.put("porkchop", "item/porkchop");
        baseTextures.put("diamond", "item/diamond");
        baseTextures.put("emerald", "item/emerald");
        baseTextures.put("gold_ingot", "item/gold_ingot");
        baseTextures.put("iron_ingot", "item/iron_ingot");
        baseTextures.put("stick", "item/stick");
        baseTextures.put("diamond_sword", "item/diamond_sword");
        baseTextures.put("diamond_pickaxe", "item/diamond_pickaxe");
        baseTextures.put("netherite_sword", "item/netherite_sword");
        baseTextures.put("ender_pearl", "item/ender_pearl");
        baseTextures.put("blaze_rod", "item/blaze_rod");
        baseTextures.put("ender_eye", "item/ender_eye");
        baseTextures.put("experience_bottle", "item/experience_bottle");

        palette.put("Black", new int[]{0, 0, 0});
        palette.put("Dark Gray", new int[]{64, 64, 64});
        palette.put("Gray", new int[]{128, 128, 128});
        palette.put("Light Gray", new int[]{192, 192, 192});
        palette.put("White", new int[]{255, 255, 255});
        palette.put("Red", new int[]{255, 0, 0});
        palette.put("Dark Red", new int[]{128, 0, 0});
        palette.put("Orange", new int[]{255, 128, 0});
        palette.put("Yellow", new int[]{255, 255, 0});
        palette.put("Lime", new int[]{128, 255, 0});
        palette.put("Green", new int[]{0, 255, 0});
        palette.put("Dark Green", new int[]{0, 128, 0});
        palette.put("Cyan", new int[]{0, 255, 255});
        palette.put("Light Blue", new int[]{0, 128, 255});
        palette.put("Blue", new int[]{0, 0, 255});
        palette.put("Dark Blue", new int[]{0, 0, 128});
        palette.put("Purple", new int[]{128, 0, 255});
        palette.put("Magenta", new int[]{255, 0, 255});
        palette.put("Pink", new int[]{255, 128, 192});
        palette.put("Brown", new int[]{128, 64, 0});
        palette.put("Skin", new int[]{255, 200, 150});
        palette.put("Sand", new int[]{220, 200, 150});
        palette.put("Grass", new int[]{100, 180, 50});
        palette.put("Leaf", new int[]{50, 140, 30});
        palette.put("Wood", new int[]{120, 80, 40});
        palette.put("Stone", new int[]{128, 128, 128});
        palette.put("Water", new int[]{30, 100, 200});
        palette.put("Lava", new int[]{200, 80, 0});
        palette.put("Fire", new int[]{255, 100, 0});
        palette.put("Glow", new int[]{255, 255, 150});
    }

    public static TextureData createTexture(String name) {
        TextureData tex = new TextureData(name);
        textures.put(name, tex);
        return tex;
    }

    public static TextureData createTextureFromBase(String name, String baseName) {
        TextureData tex = new TextureData(name);
        tex.setBaseTexture(baseName);
        BufferedImage base = loadBaseTexture(baseName);
        if (base != null) tex.loadFromImage(base);
        textures.put(name, tex);
        return tex;
    }

    public static TextureData getTexture(String name) { return textures.get(name); }
    public static Collection<TextureData> getAllTextures() { return textures.values(); }
    public static void removeTexture(String name) { textures.remove(name); }
    public static Map<String, String> getBaseTextures() { return baseTextures; }
    public static Map<String, int[]> getPalette() { return palette; }

    public static BufferedImage loadBaseTexture(String baseName) {
        return createPlaceholderImage(baseName);
    }

    private static BufferedImage createPlaceholderImage(String name) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int hash = name.hashCode();
        int r = Math.abs(hash) % 200 + 30;
        int gr = Math.abs(hash / 7) % 200 + 30;
        int b = Math.abs(hash / 13) % 200 + 30;
        Color c = new Color(r, gr, b);
        g.setColor(c);
        g.fillRect(0, 0, 16, 16);
        g.setColor(c.darker().darker());
        g.drawRect(0, 0, 15, 15);
        g.setColor(c.brighter());
        g.drawLine(2, 2, 13, 2);
        g.drawLine(2, 2, 2, 13);
        g.dispose();
        return img;
    }

    public static void saveTexture(TextureData texture) {
        try {
            MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            Path texDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("modscript/textures");
            Files.createDirectories(texDir);
            BufferedImage image = texture.toImage();
            File outFile = texDir.resolve(texture.getName() + ".png").toFile();
            ImageIO.write(image, "png", outFile);
            texture.setModified(false);
        } catch (Exception e) {
            System.err.println("ModScript: Failed to save texture: " + e.getMessage());
        }
    }

    public static void saveAll() {
        for (TextureData tex : textures.values()) {
            if (tex.isModified()) saveTexture(tex);
        }
    }

    public static void loadAll() {
        try {
            MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            Path texDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("modscript/textures");
            if (!Files.exists(texDir)) return;
            try (var stream = Files.list(texDir)) {
                stream.filter(p -> p.toString().endsWith(".png")).forEach(path -> {
                    try {
                        String name = path.getFileName().toString().replace(".png", "");
                        BufferedImage image = ImageIO.read(path.toFile());
                        if (image != null) {
                            TextureData tex = new TextureData(name, image);
                            textures.put(name, tex);
                        }
                    } catch (Exception e) {
                        System.err.println("ModScript: Failed to load texture: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("ModScript: Failed to load textures: " + e.getMessage());
        }
    }

    public static List<String> searchTextures(String query) {
        String q = query.toLowerCase();
        List<String> results = new ArrayList<>();
        for (String key : baseTextures.keySet()) {
            if (key.contains(q)) results.add(key);
        }
        return results;
    }
}
