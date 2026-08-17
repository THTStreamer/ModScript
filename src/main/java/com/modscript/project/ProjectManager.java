package com.modscript.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path projectsDir;

    public static void init(Path worldDir) {
        projectsDir = worldDir.resolve("modscript").resolve("projects");
        try {
            Files.createDirectories(projectsDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create projects directory", e);
        }
    }

    public static Path getProjectsDir() {
        return projectsDir;
    }

    public static Project createProject(String name) throws IOException {
        Path projectDir = projectsDir.resolve(name);
        if (Files.exists(projectDir)) {
            throw new IOException("Project already exists: " + name);
        }

        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("scripts"));
        Files.createDirectories(projectDir.resolve("items"));
        Files.createDirectories(projectDir.resolve("blocks"));

        Project project = new Project(name, "1.0.0", System.currentTimeMillis());
        Files.writeString(projectDir.resolve("project.json"), GSON.toJson(project));

        return project;
    }

    public static Project loadProject(String name) throws IOException {
        Path projectDir = projectsDir.resolve(name);
        Path projectFile = projectDir.resolve("project.json");

        if (!Files.exists(projectFile)) {
            throw new IOException("Project not found: " + name);
        }

        String json = Files.readString(projectFile);
        return GSON.fromJson(json, Project.class);
    }

    public static List<String> listProjects() throws IOException {
        List<String> projects = new ArrayList<>();
        if (Files.exists(projectsDir)) {
            try (var stream = Files.list(projectsDir)) {
                stream.filter(Files::isDirectory)
                    .forEach(path -> projects.add(path.getFileName().toString()));
            }
        }
        return projects;
    }

    public static String loadScript(String projectName) {
        try {
            Path scriptFile = projectsDir.resolve(projectName).resolve("scripts").resolve("main.ms");
            if (Files.exists(scriptFile)) return Files.readString(scriptFile);
        } catch (Exception e) {}
        return null;
    }

    public static void saveScript(String projectName, String script) {
        try {
            Path scriptsDir = projectsDir.resolve(projectName).resolve("scripts");
            Files.createDirectories(scriptsDir);
            Files.writeString(scriptsDir.resolve("main.ms"), script);
        } catch (Exception e) {}
    }

    public static record Project(
        String name,
        String version,
        long created
    ) {}
}
