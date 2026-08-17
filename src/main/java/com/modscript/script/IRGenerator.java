package com.modscript.script;

import java.util.*;

public class IRGenerator {
    private final List<IRInstruction> instructions = new ArrayList<>();
    private int tempCounter = 0;

    public List<IRInstruction> generate(ASTNode root) {
        instructions.clear();
        tempCounter = 0;
        if (root instanceof ASTNode.Program program) {
            for (var stmt : program.getStatements()) generateNode(stmt);
        }
        return instructions;
    }

    private void generateNode(ASTNode node) {
        if (node instanceof ASTNode.CreateItemStatement create) {
            instructions.add(new IRInstruction(IROpcode.CREATE_ITEM,
                    create.getName(), extractProperties(create.getProperties())));
        } else if (node instanceof ASTNode.CreateBlockStatement create) {
            instructions.add(new IRInstruction(IROpcode.CREATE_BLOCK,
                    create.getName(), extractProperties(create.getProperties())));
        } else if (node instanceof ASTNode.CreateMobStatement create) {
            instructions.add(new IRInstruction(IROpcode.CREATE_MOB,
                    create.getName(), extractProperties(create.getProperties())));
        } else if (node instanceof ASTNode.CreateEffectStatement create) {
            instructions.add(new IRInstruction(IROpcode.CREATE_EFFECT,
                    create.getName(), extractProperties(create.getProperties())));
        } else if (node instanceof ASTNode.CreateRecipeStatement create) {
            instructions.add(new IRInstruction(IROpcode.CREATE_RECIPE,
                    create.getName(), extractProperties(create.getProperties())));
        } else if (node instanceof ASTNode.CreateAbilityStatement create) {
            instructions.add(new IRInstruction(IROpcode.CREATE_ABILITY,
                    create.getName(), extractProperties(create.getProperties())));
        } else if (node instanceof ASTNode.WhenStatement when) {
            String label = "event_" + when.getEvent().replaceAll("\\s+", "_");
            instructions.add(new IRInstruction(IROpcode.LABEL, label, null));
            for (var action : when.getActions()) generateNode(action);
            instructions.add(new IRInstruction(IROpcode.END_EVENT, label, null));
        } else if (node instanceof ASTNode.ActionNode action) {
            generateAction(action);
        }
    }

    private void generateAction(ASTNode.ActionNode action) {
        switch (action.getType()) {
            case "give" -> {
                String t = "t" + (tempCounter++);
                instructions.add(new IRInstruction(IROpcode.MOV, t, action.getParameters().get(0)));
                instructions.add(new IRInstruction(IROpcode.GIVE, t, action.getParameters().size() > 1 ? action.getParameters().get(1) : null));
            }
            case "teleport" -> {
                String t1 = "t" + (tempCounter++);
                String t2 = "t" + (tempCounter++);
                instructions.add(new IRInstruction(IROpcode.MOV, t1, action.getParameters().get(0)));
                instructions.add(new IRInstruction(IROpcode.MOV, t2, action.getParameters().size() > 1 ? action.getParameters().get(1) : null));
                instructions.add(new IRInstruction(IROpcode.TELEPORT, t1, t2));
            }
            case "spawn", "summon" -> {
                String t = "t" + (tempCounter++);
                instructions.add(new IRInstruction(IROpcode.MOV, t, action.getParameters().get(0)));
                instructions.add(new IRInstruction(IROpcode.SPAWN, t, null));
            }
            case "remove" -> {
                String t = "t" + (tempCounter++);
                instructions.add(new IRInstruction(IROpcode.MOV, t, action.getParameters().get(0)));
                instructions.add(new IRInstruction(IROpcode.REMOVE, t, null));
            }
            case "apply" -> {
                String t = "t" + (tempCounter++);
                String d = "t" + (tempCounter++);
                instructions.add(new IRInstruction(IROpcode.MOV, t, action.getParameters().get(0)));
                instructions.add(new IRInstruction(IROpcode.MOV, d, action.getParameters().size() > 1 ? action.getParameters().get(1) : null));
                instructions.add(new IRInstruction(IROpcode.APPLY_EFFECT, t, d));
            }
            case "heal" -> {
                String t = "t" + (tempCounter++);
                instructions.add(new IRInstruction(IROpcode.MOV, t, action.getParameters().get(0)));
                instructions.add(new IRInstruction(IROpcode.HEAL, t, null));
            }
            case "shoot" -> {
                instructions.add(new IRInstruction(IROpcode.SHOOT, null, null));
            }
            case "deal" -> {
                String t = "t" + (tempCounter++);
                instructions.add(new IRInstruction(IROpcode.MOV, t, action.getParameters().get(0)));
                instructions.add(new IRInstruction(IROpcode.DEAL_DAMAGE, t, null));
            }
            case "play" -> {
                String t = "t" + (tempCounter++);
                instructions.add(new IRInstruction(IROpcode.MOV, t, action.getParameters().get(0)));
                instructions.add(new IRInstruction(IROpcode.PLAY_SOUND, t, null));
            }
            case "set" -> {
                instructions.add(new IRInstruction(IROpcode.SET, null, null));
            }
        }
    }

    private Map<String, Object> extractProperties(List<ASTNode.PropertyNode> props) {
        Map<String, Object> map = new HashMap<>();
        for (var prop : props) {
            if (prop.getValue() instanceof ASTNode.StringLiteral s) map.put(prop.getKey(), s.getValue());
            else if (prop.getValue() instanceof ASTNode.NumberLiteral n) map.put(prop.getKey(), n.getValue());
            else if (prop.getValue() instanceof ASTNode.BooleanLiteral b) map.put(prop.getKey(), b.isValue());
        }
        return map;
    }

    public enum IROpcode {
        CREATE_ITEM, CREATE_BLOCK, CREATE_MOB, CREATE_EFFECT, CREATE_RECIPE, CREATE_ABILITY,
        LABEL, END_EVENT, MOV, GIVE, TELEPORT, SPAWN, REMOVE, APPLY_EFFECT, HEAL,
        SHOOT, DEAL_DAMAGE, PLAY_SOUND, SET
    }

    public record IRInstruction(IROpcode opcode, Object operand1, Object operand2) {
        @Override
        public String toString() {
            return opcode + " " + (operand1 != null ? operand1 : "") + " " + (operand2 != null ? operand2 : "");
        }
    }
}
