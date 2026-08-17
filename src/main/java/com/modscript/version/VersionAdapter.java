package com.modscript.version;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import java.util.*;

public class VersionAdapter {
    private static String detectedVersion = null;
    private static String neoForgeVersion = null;
    private static final Map<String, VersionCompat> compatMap = new HashMap<>();

    static {
        compatMap.put("1.21.1", new VersionCompat("1.21.1", "21.1", true, true, true));
        compatMap.put("1.21", new VersionCompat("1.21", "21.0", true, true, false));
        compatMap.put("1.20.6", new VersionCompat("1.20.6", "20.6", true, false, false));
        compatMap.put("1.20.4", new VersionCompat("1.20.4", "20.4", true, false, false));
    }

    public static String detectVersion() {
        if (detectedVersion != null) return detectedVersion;
        detectedVersion = "1.21.1";
        return detectedVersion;
    }

    public static String getNeoForgeVersion() {
        if (neoForgeVersion != null) return neoForgeVersion;
        try {
            neoForgeVersion = FMLLoader.versionInfo().neoForgeVersion();
            return neoForgeVersion;
        } catch (Exception e) {}
        neoForgeVersion = "21.1.248";
        return neoForgeVersion;
    }

    public static VersionCompat getCompat() {
        String ver = detectVersion();
        return compatMap.getOrDefault(ver, new VersionCompat(ver, "unknown", true, true, true));
    }

    public static boolean supportsPotions() { return getCompat().potionSupport; }
    public static boolean supportsAttributes() { return getCompat().attributeSupport; }
    public static boolean supportsDataComponents() { return getCompat().dataComponentSupport; }

    public static String getVersionInfo() {
        return String.format("Minecraft %s | NeoForge %s | ModScript 1.0", detectVersion(), getNeoForgeVersion());
    }

    public static List<String> getSupportedVersions() { return new ArrayList<>(compatMap.keySet()); }

    public record VersionCompat(String mcVersion, String neoForgeApi, boolean potionSupport,
                                 boolean attributeSupport, boolean dataComponentSupport) {}
}
