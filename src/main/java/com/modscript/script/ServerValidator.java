package com.modscript.script;

import com.modscript.registry.ModScriptRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.regex.Pattern;

public class ServerValidator {
    private static final int MAX_SCRIPT_LENGTH = 50000;
    private static final int MAX_DEFINITIONS = 100;
    private static final int MAX_ACTIONS_PER_EVENT = 50;
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_ ]{1,64}$");
    private static final Set<String> RESTRICTED_NAMES = Set.of("minecraft", "forge", "neoforge", "modscript", "admin", "op");

    public static ValidationResult validate(String script, ServerPlayer player) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (script.length() > MAX_SCRIPT_LENGTH) {
            errors.add("Script exceeds maximum length (" + MAX_SCRIPT_LENGTH + " chars)");
            return new ValidationResult(false, errors, warnings);
        }

        if (!PermissionSystem.canCreateMod(player)) {
            errors.add("You don't have permission to create mods");
            return new ValidationResult(false, errors, warnings);
        }

        try {
            ModScriptLexer lexer = new ModScriptLexer(script);
            var tokens = lexer.tokenize();
            ModScriptParser parser = new ModScriptParser(tokens);
            var ast = parser.parse();

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            List<String> semanticErrors = analyzer.analyze(ast);
            errors.addAll(semanticErrors);
            warnings.addAll(analyzer.getWarnings());

            IRGenerator irGen = new IRGenerator();
            var instructions = irGen.generate(ast);

            ScriptSandbox.validateIR(instructions);

            int defCount = 0;
            for (var instr : instructions) {
                if (instr.opcode().name().startsWith("CREATE_")) {
                    defCount++;
                    String name = (String) instr.operand1();
                    if (name != null && !SAFE_NAME_PATTERN.matcher(name).matches()) {
                        errors.add("Invalid name: '" + name + "' - only letters, numbers, spaces, underscores allowed (max 64 chars)");
                    }
                    if (name != null && RESTRICTED_NAMES.contains(name.toLowerCase())) {
                        errors.add("Restricted name: '" + name + "'");
                    }
                }
            }

            if (defCount > MAX_DEFINITIONS) {
                errors.add("Too many definitions (max " + MAX_DEFINITIONS + ")");
            }

            Map<String, Integer> eventCounts = new HashMap<>();
            for (var instr : instructions) {
                if (instr.opcode() == IRGenerator.IROpcode.LABEL) {
                    String event = (String) instr.operand1();
                    eventCounts.merge(event, 1, Integer::sum);
                }
            }
            for (var entry : eventCounts.entrySet()) {
                if (entry.getValue() > MAX_ACTIONS_PER_EVENT) {
                    warnings.add("Event '" + entry.getKey() + "' has " + entry.getValue() + " handlers (recommended max " + MAX_ACTIONS_PER_EVENT + ")");
                }
            }

        } catch (ModScriptParser.ScriptException e) {
            errors.add("Parse error: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Validation error: " + e.getMessage());
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    public static boolean validateName(String name) {
        return name != null && SAFE_NAME_PATTERN.matcher(name).matches() && !RESTRICTED_NAMES.contains(name.toLowerCase());
    }

    public static boolean validateScriptLength(String script) {
        return script != null && script.length() <= MAX_SCRIPT_LENGTH;
    }

    public record ValidationResult(boolean valid, List<String> errors, List<String> warnings) {}
}
