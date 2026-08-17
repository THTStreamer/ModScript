package com.modscript.script;

import java.util.*;

public class SemanticAnalyzer {
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Set<String> definedNames = new HashSet<>();
    private final Set<String> usedNames = new HashSet<>();

    public List<String> analyze(ASTNode root) {
        errors.clear();
        warnings.clear();
        definedNames.clear();
        usedNames.clear();
        if (root instanceof ASTNode.Program program) {
            for (var stmt : program.getStatements()) analyzeNode(stmt);
            for (var name : usedNames) {
                if (!definedNames.contains(name) && !isBuiltin(name)) {
                    warnings.add("Undefined name used: " + name);
                }
            }
        }
        return errors;
    }

    private void analyzeNode(ASTNode node) {
        if (node instanceof ASTNode.CreateItemStatement create) {
            if (definedNames.contains(create.getName())) {
                errors.add("Line " + create.getLine() + ": Item '" + create.getName() + "' already defined");
            }
            definedNames.add(create.getName());
            for (var prop : create.getProperties()) validateProperty(prop);
        } else if (node instanceof ASTNode.CreateBlockStatement create) {
            if (definedNames.contains(create.getName())) {
                errors.add("Line " + create.getLine() + ": Block '" + create.getName() + "' already defined");
            }
            definedNames.add(create.getName());
        } else if (node instanceof ASTNode.CreateMobStatement create) {
            if (definedNames.contains(create.getName())) {
                errors.add("Line " + create.getLine() + ": Mob '" + create.getName() + "' already defined");
            }
            definedNames.add(create.getName());
            validateMobProperties(create.getProperties());
        } else if (node instanceof ASTNode.CreateEffectStatement create) {
            if (definedNames.contains(create.getName())) {
                errors.add("Line " + create.getLine() + ": Effect '" + create.getName() + "' already defined");
            }
            definedNames.add(create.getName());
        } else if (node instanceof ASTNode.CreateRecipeStatement create) {
            validateRecipe(create);
        } else if (node instanceof ASTNode.CreateAbilityStatement create) {
            if (definedNames.contains(create.getName())) {
                errors.add("Line " + create.getLine() + ": Ability '" + create.getName() + "' already defined");
            }
            definedNames.add(create.getName());
        } else if (node instanceof ASTNode.WhenStatement when) {
            validateEvent(when.getEvent(), when.getLine());
            for (var action : when.getActions()) analyzeNode(action);
        } else if (node instanceof ASTNode.ActionNode action) {
            validateAction(action);
        }
    }

    private void validateProperty(ASTNode.PropertyNode prop) {
        String key = prop.getKey();
        if (!isValidItemProperty(key)) warnings.add("Unknown property: " + key);
        if (prop.getValue() instanceof ASTNode.StringLiteral str) usedNames.add(str.getValue());
    }

    private boolean isValidItemProperty(String key) {
        return Set.of("damage", "durability", "speed", "attack", "heal", "food", "color", "rarity").contains(key);
    }

    private void validateMobProperties(List<ASTNode.PropertyNode> props) {
        for (var prop : props) {
            if (!Set.of("health", "attack", "speed", "type", "base", "model", " drops").contains(prop.getKey())) {
                warnings.add("Unknown mob property: " + prop.getKey());
            }
        }
    }

    private void validateRecipe(ASTNode.CreateRecipeStatement recipe) {
        for (var prop : recipe.getProperties()) {
            if (prop.getKey().equals("pattern")) {
                if (prop.getValue() instanceof ASTNode.StringLiteral str) {
                    String[] rows = str.getValue().split("\\|");
                    if (rows.length > 3) errors.add("Recipe pattern cannot have more than 3 rows");
                    for (String row : rows) if (row.length() > 3) errors.add("Recipe row cannot have more than 3 characters");
                }
            }
        }
    }

    private void validateEvent(String event, int line) {
        if (!isValidEvent(event)) {
            errors.add("Line " + line + ": Unknown event '" + event + "'");
        }
    }

    private boolean isValidEvent(String event) {
        String e = event.toLowerCase();
        return e.contains("attack") || e.contains("break") || e.contains("join")
                || e.contains("click") || e.contains("place") || e.contains("hurt")
                || e.contains("die") || e.contains("sneak") || e.contains("jump")
                || e.contains("swim") || e.contains("craft") || e.contains("eat")
                || e.contains("interact");
    }

    private void validateAction(ASTNode.ActionNode action) {
        if (action.getType().equals("give") && action.getParameters().size() < 2) {
            errors.add("Line " + action.getLine() + ": 'give' requires quantity and item name");
        }
        if (action.getType().equals("teleport") && action.getParameters().size() < 2) {
            errors.add("Line " + action.getLine() + ": 'teleport' requires coordinates");
        }
        if (action.getType().equals("deal") && action.getParameters().isEmpty()) {
            errors.add("Line " + action.getLine() + ": 'deal' requires damage amount");
        }
    }

    private boolean isBuiltin(String name) {
        return Set.of("zombie", "skeleton", "creeper", "spider", "enderman", "speed", "strength",
                "regeneration", "invisibility", "fire resistance", "healing", "poison", "weakness",
                "slowness", "haste", "jump boost", "absorption", "health boost", "night vision",
                "player", "self").contains(name);
    }

    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
}
