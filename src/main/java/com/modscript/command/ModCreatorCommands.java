package com.modscript.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.modscript.ai.AIAssistant;
import com.modscript.debugger.ScriptDebugger;
import com.modscript.debugger.ScriptProfiler;
import com.modscript.project.ModExporter;
import com.modscript.project.ProjectManager;
import com.modscript.project.ProjectVersioning;
import com.modscript.script.ServerValidator;
import com.modscript.script.ScriptRuntime;
import com.modscript.version.VersionAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.List;

public class ModCreatorCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("modcreator")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> createProject(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("list")
                .executes(ctx -> listProjects(ctx.getSource())))
            .then(Commands.literal("open")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> openProject(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("save")
                .then(Commands.argument("project", StringArgumentType.word())
                    .then(Commands.argument("script", StringArgumentType.greedyString())
                        .executes(ctx -> saveScript(ctx.getSource(), StringArgumentType.getString(ctx, "project"), StringArgumentType.getString(ctx, "script"))))))
            .then(Commands.literal("run")
                .then(Commands.argument("project", StringArgumentType.word())
                    .then(Commands.argument("script", StringArgumentType.greedyString())
                        .executes(ctx -> runScript(ctx.getSource(), StringArgumentType.getString(ctx, "project"), StringArgumentType.getString(ctx, "script"))))))
            .then(Commands.literal("undo")
                .then(Commands.argument("project", StringArgumentType.word())
                    .executes(ctx -> undo(ctx.getSource(), StringArgumentType.getString(ctx, "project")))))
            .then(Commands.literal("redo")
                .then(Commands.argument("project", StringArgumentType.word())
                    .executes(ctx -> redo(ctx.getSource(), StringArgumentType.getString(ctx, "project")))))
            .then(Commands.literal("versions")
                .then(Commands.argument("project", StringArgumentType.word())
                    .executes(ctx -> listVersions(ctx.getSource(), StringArgumentType.getString(ctx, "project")))))
            .then(Commands.literal("export")
                .then(Commands.argument("project", StringArgumentType.word())
                    .then(Commands.argument("script", StringArgumentType.greedyString())
                        .executes(ctx -> exportMod(ctx.getSource(), StringArgumentType.getString(ctx, "project"), StringArgumentType.getString(ctx, "script"))))))
            .then(Commands.literal("debug")
                .then(Commands.argument("project", StringArgumentType.word())
                    .executes(ctx -> debugScript(ctx.getSource(), StringArgumentType.getString(ctx, "project")))))
            .then(Commands.literal("profile")
                .then(Commands.argument("project", StringArgumentType.word())
                    .executes(ctx -> profileScript(ctx.getSource(), StringArgumentType.getString(ctx, "project")))))
            .then(Commands.literal("ai")
                .then(Commands.literal("generate")
                    .then(Commands.argument("description", StringArgumentType.greedyString())
                        .executes(ctx -> aiGenerate(ctx.getSource(), StringArgumentType.getString(ctx, "description")))))
                .then(Commands.literal("fix")
                    .then(Commands.argument("script", StringArgumentType.greedyString())
                        .executes(ctx -> aiFix(ctx.getSource(), StringArgumentType.getString(ctx, "script")))))
                .then(Commands.literal("explain")
                    .then(Commands.argument("script", StringArgumentType.greedyString())
                        .executes(ctx -> aiExplain(ctx.getSource(), StringArgumentType.getString(ctx, "script")))))
                .then(Commands.literal("tutorial")
                    .executes(ctx -> aiTutorial(ctx.getSource()))))
            .then(Commands.literal("version")
                .executes(ctx -> showVersion(ctx.getSource())))
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "ModScript v1.0 - In-Game Mod Creator\n" +
                    "  create <name> - New project\n" +
                    "  list - All projects\n" +
                    "  open <name> - Open project\n" +
                    "  save <project> <script> - Save script\n" +
                    "  run <project> <script> - Run script\n" +
                    "  undo/redo <project> - Version control\n" +
                    "  export <project> <script> - Export as .jar\n" +
                    "  debug <project> - Debug script\n" +
                    "  profile <project> - Profile performance\n" +
                    "  ai generate/fix/explain/tutorial - AI assistant\n" +
                    "  version - Show MC/NeoForge version"), false);
                return 0;
            })
        );
    }

    private static int createProject(CommandSourceStack source, String name) {
        try {
            ProjectManager.createProject(name);
            source.sendSuccess(() -> Component.literal("Created project: " + name), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int listProjects(CommandSourceStack source) {
        try {
            List<String> projects = ProjectManager.listProjects();
            if (projects.isEmpty()) { source.sendSuccess(() -> Component.literal("No projects yet."), false); }
            else { source.sendSuccess(() -> Component.literal("Projects:"), false); projects.forEach(p -> source.sendSuccess(() -> Component.literal("  - " + p), false)); }
            return 1;
        } catch (Exception e) { source.sendFailure(Component.literal("Failed: " + e.getMessage())); return 0; }
    }

    private static int openProject(CommandSourceStack source, String name) {
        try { ProjectManager.loadProject(name); source.sendSuccess(() -> Component.literal("Opened: " + name), true); return 1; }
        catch (Exception e) { source.sendFailure(Component.literal("Failed: " + e.getMessage())); return 0; }
    }

    private static int saveScript(CommandSourceStack source, String project, String script) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        String decoded = script.replace("\\n", "\n");
        ProjectVersioning.saveVersion(project, decoded, player.getName().getString());
        player.sendSystemMessage(Component.literal("Script saved (version " + (ProjectVersioning.getVersionHistory(project).size()) + ")"));
        return 1;
    }

    private static int runScript(CommandSourceStack source, String project, String script) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        String decoded = script.replace("\\n", "\n");
        ProjectVersioning.saveVersion(project, decoded, player.getName().getString());
        ScriptRuntime.compileAndRun(project, decoded, player);
        return 1;
    }

    private static int undo(CommandSourceStack source, String project) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        String script = ProjectVersioning.undo(project);
        if (script != null) { player.sendSystemMessage(Component.literal("Undone. Run with current script to apply.")); return 1; }
        else { player.sendSystemMessage(Component.literal("Nothing to undo.")); return 0; }
    }

    private static int redo(CommandSourceStack source, String project) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        String script = ProjectVersioning.redo(project);
        if (script != null) { player.sendSystemMessage(Component.literal("Redone.")); return 1; }
        else { player.sendSystemMessage(Component.literal("Nothing to redo.")); return 0; }
    }

    private static int listVersions(CommandSourceStack source, String project) {
        var versions = ProjectVersioning.getVersionHistory(project);
        if (versions.isEmpty()) { source.sendSuccess(() -> Component.literal("No versions saved."), false); return 1; }
        source.sendSuccess(() -> Component.literal("Version History (" + versions.size() + "):"), false);
        for (var v : versions) {
            source.sendSuccess(() -> Component.literal(String.format("  v%d by %s - %s", v.version(), v.author(), v.preview())), false);
        }
        return 1;
    }

    private static int exportMod(CommandSourceStack source, String project, String script) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        String decoded = script.replace("\\n", "\n");
        var validation = ModExporter.validateForExport(decoded);
        if (!validation.isEmpty()) { validation.forEach(e -> player.sendSystemMessage(Component.literal("§c" + e))); return 0; }
        try {
            var result = ModExporter.exportToJar(project, decoded, null);
            player.sendSystemMessage(Component.literal("§aExported! Mod ID: " + result.modId()));
            player.sendSystemMessage(Component.literal("Jar: " + result.jarPath()));
            return 1;
        } catch (Exception e) { player.sendSystemMessage(Component.literal("§cExport failed: " + e.getMessage())); return 0; }
    }

    private static int debugScript(CommandSourceStack source, String project) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        var session = ScriptDebugger.startDebug(project, "");
        player.sendSystemMessage(Component.literal("Debug session started: " + session.getId()));
        var events = ScriptDebugger.step(session.getId());
        events.forEach(e -> player.sendSystemMessage(Component.literal("[" + e.type() + "] " + e.message())));
        ScriptDebugger.stopDebug(session.getId());
        return 1;
    }

    private static int profileScript(CommandSourceStack source, String project) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        var profiler = ScriptProfiler.startProfiling(project);
        ScriptProfiler.captureMemory(profiler.id);
        long start = System.nanoTime();
        try { ScriptRuntime.compileAndRun(project, "", player); } catch (Exception e) {}
        ScriptProfiler.recordExecution(profiler.id, "total", System.nanoTime() - start);
        ScriptProfiler.captureMemory(profiler.id);
        ScriptProfiler.endProfiling(profiler.id);
        var report = ScriptProfiler.getReport(profiler.id);
        if (report != null) player.sendSystemMessage(Component.literal(report.toString()));
        return 1;
    }

    private static int aiGenerate(CommandSourceStack source, String description) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        String code = AIAssistant.generateCode(description);
        player.sendSystemMessage(Component.literal("Generated:\n" + code));
        return 1;
    }

    private static int aiFix(CommandSourceStack source, String script) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        var result = ServerValidator.validate(script.replace("\\n", "\n"), player);
        if (result.valid()) { player.sendSystemMessage(Component.literal("§aNo errors found!")); return 1; }
        player.sendSystemMessage(Component.literal("§cErrors:"));
        result.errors().forEach(e -> {
            String fix = AIAssistant.suggestFix(e);
            player.sendSystemMessage(Component.literal("  §c" + e + "\n  §aSuggestion: " + fix));
        });
        return 1;
    }

    private static int aiExplain(CommandSourceStack source, String script) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        String explanation = AIAssistant.explainScript(script.replace("\\n", "\n"));
        player.sendSystemMessage(Component.literal(explanation));
        return 1;
    }

    private static int aiTutorial(CommandSourceStack source) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception e) { source.sendFailure(Component.literal("Must be run by a player")); return 0; }
        for (String t : AIAssistant.getTutorial()) player.sendSystemMessage(Component.literal(t));
        return 1;
    }

    private static int showVersion(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(VersionAdapter.getVersionInfo()), false);
        source.sendSuccess(() -> Component.literal("Supported: " + VersionAdapter.getSupportedVersions()), false);
        return 1;
    }
}
