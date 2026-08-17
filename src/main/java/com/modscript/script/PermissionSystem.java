package com.modscript.script;

import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionSystem {
    private static final Map<String, Set<String>> playerPermissions = new ConcurrentHashMap<>();
    private static final Set<String> defaultPermissions = Set.of("modscript.view", "modscript.run");
    private static final Set<String> opPermissions = Set.of("modscript.view", "modscript.run", "modscript.create",
            "modscript.edit", "modscript.delete", "modscript.admin", "modscript.export");

    public static void loadPermissions(Path worldPath) {
        try {
            Path permFile = worldPath.resolve("modscript/permissions.json");
            if (Files.exists(permFile)) {
                String json = Files.readString(permFile);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                for (var entry : obj.entrySet()) {
                    Set<String> perms = new HashSet<>();
                    entry.getValue().getAsJsonArray().forEach(e -> perms.add(e.getAsString()));
                    playerPermissions.put(entry.getKey(), perms);
                }
            }
        } catch (Exception e) {
            System.err.println("ModScript: Failed to load permissions: " + e.getMessage());
        }
    }

    public static void savePermissions(Path worldPath) {
        try {
            Path permFile = worldPath.resolve("modscript/permissions.json");
            Files.createDirectories(permFile.getParent());
            JsonObject json = new JsonObject();
            for (var entry : playerPermissions.entrySet()) {
                JsonArray arr = new JsonArray();
                entry.getValue().forEach(arr::add);
                json.add(entry.getKey(), arr);
            }
            Files.writeString(permFile, new GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (Exception e) {
            System.err.println("ModScript: Failed to save permissions: " + e.getMessage());
        }
    }

    public static boolean hasPermission(ServerPlayer player, String permission) {
        if (player.hasPermissions(2)) return true;
        Set<String> perms = playerPermissions.get(player.getUUID().toString());
        if (perms == null) perms = new HashSet<>(defaultPermissions);
        return perms.contains(permission) || perms.contains("modscript.admin");
    }

    public static void grantPermission(String uuid, String permission) {
        playerPermissions.computeIfAbsent(uuid, k -> new HashSet<>()).add(permission);
    }

    public static void revokePermission(String uuid, String permission) {
        Set<String> perms = playerPermissions.get(uuid);
        if (perms != null) perms.remove(permission);
    }

    public static void setPermissions(String uuid, Set<String> permissions) {
        playerPermissions.put(uuid, new HashSet<>(permissions));
    }

    public static Set<String> getPermissions(String uuid) {
        return playerPermissions.getOrDefault(uuid, new HashSet<>(defaultPermissions));
    }

    public static boolean canCreateMod(ServerPlayer player) { return hasPermission(player, "modscript.create"); }
    public static boolean canEditMod(ServerPlayer player) { return hasPermission(player, "modscript.edit"); }
    public static boolean canDeleteMod(ServerPlayer player) { return hasPermission(player, "modscript.delete"); }
    public static boolean canRunMod(ServerPlayer player) { return hasPermission(player, "modscript.run"); }
    public static boolean canExportMod(ServerPlayer player) { return hasPermission(player, "modscript.export"); }
    public static boolean isAdmin(ServerPlayer player) { return hasPermission(player, "modscript.admin"); }

    public static void syncToClient(ServerPlayer player) {
        Set<String> perms = getPermissions(player.getUUID().toString());
        String permString = String.join(",", perms);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[ModScript] Permissions: " + permString));
    }
}
