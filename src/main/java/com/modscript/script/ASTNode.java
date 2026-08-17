package com.modscript.script;

import java.util.List;
import java.util.Map;

public abstract class ASTNode {
    private final int line;
    private final int column;

    protected ASTNode(int line, int column) { this.line = line; this.column = column; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    public static class Program extends ASTNode {
        private final List<ASTNode> statements;
        public Program(List<ASTNode> statements, int line, int column) { super(line, column); this.statements = statements; }
        public List<ASTNode> getStatements() { return statements; }
    }

    public static class CreateItemStatement extends ASTNode {
        private final String name;
        private final List<PropertyNode> properties;
        public CreateItemStatement(String name, List<PropertyNode> properties, int line, int column) { super(line, column); this.name = name; this.properties = properties; }
        public String getName() { return name; }
        public List<PropertyNode> getProperties() { return properties; }
    }

    public static class CreateBlockStatement extends ASTNode {
        private final String name;
        private final List<PropertyNode> properties;
        public CreateBlockStatement(String name, List<PropertyNode> properties, int line, int column) { super(line, column); this.name = name; this.properties = properties; }
        public String getName() { return name; }
        public List<PropertyNode> getProperties() { return properties; }
    }

    public static class CreateMobStatement extends ASTNode {
        private final String name;
        private final List<PropertyNode> properties;
        public CreateMobStatement(String name, List<PropertyNode> properties, int line, int column) { super(line, column); this.name = name; this.properties = properties; }
        public String getName() { return name; }
        public List<PropertyNode> getProperties() { return properties; }
    }

    public static class CreateEffectStatement extends ASTNode {
        private final String name;
        private final List<PropertyNode> properties;
        public CreateEffectStatement(String name, List<PropertyNode> properties, int line, int column) { super(line, column); this.name = name; this.properties = properties; }
        public String getName() { return name; }
        public List<PropertyNode> getProperties() { return properties; }
    }

    public static class CreateRecipeStatement extends ASTNode {
        private final String name;
        private final List<PropertyNode> properties;
        public CreateRecipeStatement(String name, List<PropertyNode> properties, int line, int column) { super(line, column); this.name = name; this.properties = properties; }
        public String getName() { return name; }
        public List<PropertyNode> getProperties() { return properties; }
    }

    public static class CreateAbilityStatement extends ASTNode {
        private final String name;
        private final List<PropertyNode> properties;
        public CreateAbilityStatement(String name, List<PropertyNode> properties, int line, int column) { super(line, column); this.name = name; this.properties = properties; }
        public String getName() { return name; }
        public List<PropertyNode> getProperties() { return properties; }
    }

    public static class PropertyNode extends ASTNode {
        private final String key;
        private final ASTNode value;
        public PropertyNode(String key, ASTNode value, int line, int column) { super(line, column); this.key = key; this.value = value; }
        public String getKey() { return key; }
        public ASTNode getValue() { return value; }
    }

    public static class WhenStatement extends ASTNode {
        private final String event;
        private final List<ASTNode> actions;
        public WhenStatement(String event, List<ASTNode> actions, int line, int column) { super(line, column); this.event = event; this.actions = actions; }
        public String getEvent() { return event; }
        public List<ASTNode> getActions() { return actions; }
    }

    public static class ActionNode extends ASTNode {
        private final String type;
        private final List<ASTNode> parameters;
        public ActionNode(String type, List<ASTNode> parameters, int line, int column) { super(line, column); this.type = type; this.parameters = parameters; }
        public String getType() { return type; }
        public List<ASTNode> getParameters() { return parameters; }
    }

    public static class StringLiteral extends ASTNode {
        private final String value;
        public StringLiteral(String value, int line, int column) { super(line, column); this.value = value; }
        public String getValue() { return value; }
    }

    public static class NumberLiteral extends ASTNode {
        private final double value;
        public NumberLiteral(double value, int line, int column) { super(line, column); this.value = value; }
        public double getValue() { return value; }
    }

    public static class BooleanLiteral extends ASTNode {
        private final boolean value;
        public BooleanLiteral(boolean value, int line, int column) { super(line, column); this.value = value; }
        public boolean isValue() { return value; }
    }
}
