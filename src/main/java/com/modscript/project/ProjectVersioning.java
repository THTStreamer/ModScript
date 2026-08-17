package com.modscript.project;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectVersioning {
    private static final int MAX_VERSIONS = 50;
    private static final Map<String, Deque<VersionEntry>> versionHistory = new ConcurrentHashMap<>();

    public static void saveVersion(String project, String script, String author) {
        versionHistory.computeIfAbsent(project, k -> new ArrayDeque<>());
        Deque<VersionEntry> history = versionHistory.get(project);
        if (history.size() >= MAX_VERSIONS) history.pollFirst();
        history.offerLast(new VersionEntry(script, author, System.currentTimeMillis(), history.size() + 1));

        try {
            MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                Path versionDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("modscript/projects/" + project + "/versions");
                Files.createDirectories(versionDir);
                int versionNum = history.size();
                Path versionFile = versionDir.resolve("v" + versionNum + ".ms");
                Files.writeString(versionFile, script);

                JsonObject meta = new JsonObject();
                meta.addProperty("version", versionNum);
                meta.addProperty("author", author);
                meta.addProperty("timestamp", System.currentTimeMillis());
                meta.addProperty("preview", script.substring(0, Math.min(200, script.length())));
                Files.writeString(versionDir.resolve("v" + versionNum + ".json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(meta));
            }
        } catch (Exception e) {
            System.err.println("ModScript: Failed to save version: " + e.getMessage());
        }
    }

    public static String undo(String project) {
        Deque<VersionEntry> history = versionHistory.get(project);
        if (history == null || history.size() <= 1) return null;
        history.pollLast();
        return history.peekLast().script();
    }

    public static String redo(String project) {
        Deque<VersionEntry> history = versionHistory.get(project);
        if (history == null) return null;
        try {
            MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                Path versionDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("modscript/projects/" + project + "/versions");
                int nextVersion = history.size() + 1;
                Path versionFile = versionDir.resolve("v" + nextVersion + ".ms");
                if (Files.exists(versionFile)) {
                    String script = Files.readString(versionFile);
                    history.offerLast(new VersionEntry(script, "redo", System.currentTimeMillis(), nextVersion));
                    return script;
                }
            }
        } catch (Exception e) {}
        return null;
    }

    public static List<VersionInfo> getVersionHistory(String project) {
        Deque<VersionEntry> history = versionHistory.getOrDefault(project, new ArrayDeque<>());
        List<VersionInfo> list = new ArrayList<>();
        for (var entry : history) {
            list.add(new VersionInfo(entry.version(), entry.author(), entry.timestamp(),
                    entry.script().substring(0, Math.min(100, entry.script().length())) + "..."));
        }
        return list;
    }

    public static String getVersion(String project, int version) {
        try {
            MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                Path versionFile = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("modscript/projects/" + project + "/versions/v" + version + ".ms");
                if (Files.exists(versionFile)) return Files.readString(versionFile);
            }
        } catch (Exception e) {}
        return null;
    }

    public static void loadHistory(String project) {
        try {
            MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) return;
            Path versionDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("modscript/projects/" + project + "/versions");
            if (!Files.exists(versionDir)) return;
            Deque<VersionEntry> history = new ArrayDeque<>();
            for (int i = 1; i <= MAX_VERSIONS; i++) {
                Path vFile = versionDir.resolve("v" + i + ".ms");
                Path mFile = versionDir.resolve("v" + i + ".json");
                if (Files.exists(vFile) && Files.exists(mFile)) {
                    String script = Files.readString(vFile);
                    String meta = Files.readString(mFile);
                    JsonObject obj = JsonParser.parseString(meta).getAsJsonObject();
                    String author = obj.has("author") ? obj.get("author").getAsString() : "unknown";
                    long timestamp = obj.has("timestamp") ? obj.get("timestamp").getAsLong() : 0;
                    history.offerLast(new VersionEntry(script, author, timestamp, i));
                } else break;
            }
            versionHistory.put(project, history);
        } catch (Exception e) {
            System.err.println("ModScript: Failed to load version history: " + e.getMessage());
        }
    }

    private record VersionEntry(String script, String author, long timestamp, int version) {}
    public record VersionInfo(int version, String author, long timestamp, String preview) {}
}
