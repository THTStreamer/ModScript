package com.modscript.script;

import com.modscript.script.ASTNode.PropertyNode;

import java.util.ArrayList;
import java.util.List;

public class ModScriptParser {
    private final List<ModScriptLexer.Token> tokens;
    private int position;

    public ModScriptParser(List<ModScriptLexer.Token> tokens) { this.tokens = tokens; this.position = 0; }

    public ASTNode parse() throws ScriptException {
        List<ASTNode> statements = new ArrayList<>();
        while (!isAtEnd()) { ASTNode stmt = parseStatement(); if (stmt != null) statements.add(stmt); }
        return new ASTNode.Program(statements, 1, 1);
    }

    private ASTNode parseStatement() throws ScriptException {
        skipNewlines();
        if (isAtEnd()) return null;
        ModScriptLexer.Token current = peek();
        return switch (current.type()) {
            case CREATE -> parseCreate();
            case WHEN -> parseWhen();
            case GIVE -> parseAction();
            case TELEPORT -> parseAction();
            case SPAWN -> parseAction();
            case REMOVE -> parseAction();
            case APPLY -> parseAction();
            case HEAL -> parseAction();
            case SUMMON -> parseAction();
            case SHOOT -> parseAction();
            case SET -> parseSetAction();
            case DEAL -> parseDealAction();
            case PLAY -> parsePlayAction();
            default -> { advance(); yield null; }
        };
    }

    private ASTNode parseCreate() throws ScriptException {
        ModScriptLexer.Token createToken = consume(ModScriptLexer.TokenType.CREATE);
        ModScriptLexer.Token typeToken = advance();
        return switch (typeToken.type()) {
            case ITEM -> parseCreateItem(createToken, typeToken);
            case BLOCK -> parseCreateBlock(createToken, typeToken);
            case MOB -> parseCreateMob(createToken, typeToken);
            case EFFECT -> parseCreateEffect(createToken, typeToken);
            case RECIPE -> parseCreateRecipe(createToken, typeToken);
            case ABILITY -> parseCreateAbility(createToken, typeToken);
            default -> throw error("Expected item, block, mob, effect, recipe, or ability after 'create'", typeToken);
        };
    }

    private ASTNode parseCreateItem(ModScriptLexer.Token createToken, ModScriptLexer.Token typeToken) throws ScriptException {
        String name = consumeString();
        List<PropertyNode> properties = parseProperties();
        return new ASTNode.CreateItemStatement(name, properties, createToken.line(), createToken.column());
    }

    private ASTNode parseCreateBlock(ModScriptLexer.Token createToken, ModScriptLexer.Token typeToken) throws ScriptException {
        String name = consumeString();
        List<PropertyNode> properties = parseProperties();
        return new ASTNode.CreateBlockStatement(name, properties, createToken.line(), createToken.column());
    }

    private ASTNode parseCreateMob(ModScriptLexer.Token createToken, ModScriptLexer.Token typeToken) throws ScriptException {
        String name = consumeString();
        List<PropertyNode> properties = parseProperties();
        return new ASTNode.CreateMobStatement(name, properties, createToken.line(), createToken.column());
    }

    private ASTNode parseCreateEffect(ModScriptLexer.Token createToken, ModScriptLexer.Token typeToken) throws ScriptException {
        String name = consumeString();
        List<PropertyNode> properties = parseProperties();
        return new ASTNode.CreateEffectStatement(name, properties, createToken.line(), createToken.column());
    }

    private ASTNode parseCreateRecipe(ModScriptLexer.Token createToken, ModScriptLexer.Token typeToken) throws ScriptException {
        String name = consumeString();
        List<PropertyNode> properties = parseProperties();
        return new ASTNode.CreateRecipeStatement(name, properties, createToken.line(), createToken.column());
    }

    private ASTNode parseCreateAbility(ModScriptLexer.Token createToken, ModScriptLexer.Token typeToken) throws ScriptException {
        String name = consumeString();
        List<PropertyNode> properties = parseProperties();
        return new ASTNode.CreateAbilityStatement(name, properties, createToken.line(), createToken.column());
    }

    private List<PropertyNode> parseProperties() throws ScriptException {
        List<PropertyNode> properties = new ArrayList<>();
        while (!isAtEnd() && !peek().type().equals(ModScriptLexer.TokenType.WHEN)
                && !peek().type().equals(ModScriptLexer.TokenType.CREATE)
                && !peek().type().equals(ModScriptLexer.TokenType.GIVE)
                && !peek().type().equals(ModScriptLexer.TokenType.TELEPORT)
                && !peek().type().equals(ModScriptLexer.TokenType.SPAWN)
                && !peek().type().equals(ModScriptLexer.TokenType.REMOVE)
                && !peek().type().equals(ModScriptLexer.TokenType.APPLY)
                && !peek().type().equals(ModScriptLexer.TokenType.HEAL)
                && !peek().type().equals(ModScriptLexer.TokenType.SUMMON)
                && !peek().type().equals(ModScriptLexer.TokenType.SHOOT)
                && !peek().type().equals(ModScriptLexer.TokenType.SET)
                && !peek().type().equals(ModScriptLexer.TokenType.DEAL)
                && !peek().type().equals(ModScriptLexer.TokenType.PLAY)) {
            skipNewlines();
            if (isAtEnd()) break;
            if (peek().type().equals(ModScriptLexer.TokenType.WORD) || peek().type().equals(ModScriptLexer.TokenType.HEALTH)
                    || peek().type().equals(ModScriptLexer.TokenType.ATTACK) || peek().type().equals(ModScriptLexer.TokenType.SPEED)
                    || peek().type().equals(ModScriptLexer.TokenType.DAMAGE) || peek().type().equals(ModScriptLexer.TokenType.FIRE)) {
                ModScriptLexer.Token keyToken = advance();
                if (peek().type().equals(ModScriptLexer.TokenType.COLON)) {
                    advance();
                    skipNewlines();
                    ASTNode value = parseValue();
                    skipNewlines();
                    properties.add(new ASTNode.PropertyNode(keyToken.value(), value, keyToken.line(), keyToken.column()));
                } else {
                    position--;
                    break;
                }
            } else break;
        }
        return properties;
    }

    private ASTNode parseValue() throws ScriptException {
        if (peek().type().equals(ModScriptLexer.TokenType.STRING)) {
            ModScriptLexer.Token t = advance();
            return new ASTNode.StringLiteral(t.value(), t.line(), t.column());
        }
        if (peek().type().equals(ModScriptLexer.TokenType.NUMBER)) {
            ModScriptLexer.Token t = advance();
            return new ASTNode.NumberLiteral(Double.parseDouble(t.value()), t.line(), t.column());
        }
        if (peek().type().equals(ModScriptLexer.TokenType.BOOLEAN_TRUE)) {
            ModScriptLexer.Token t = advance();
            return new ASTNode.BooleanLiteral(true, t.line(), t.column());
        }
        if (peek().type().equals(ModScriptLexer.TokenType.BOOLEAN_FALSE)) {
            ModScriptLexer.Token t = advance();
            return new ASTNode.BooleanLiteral(false, t.line(), t.column());
        }
        StringBuilder sb = new StringBuilder();
        int line = peek().line(), col = peek().column();
        while (!isAtEnd() && !peek().type().equals(ModScriptLexer.TokenType.COLON)
                && !peek().type().equals(ModScriptLexer.TokenType.NEWLINE)
                && !peek().type().equals(ModScriptLexer.TokenType.EOF)
                && !peek().type().equals(ModScriptLexer.TokenType.COMMA)) {
            ModScriptLexer.Token t = advance();
            if (sb.length() > 0) sb.append(" ");
            sb.append(t.value());
        }
        if (sb.length() == 0) {
            ModScriptLexer.Token t = advance();
            return new ASTNode.StringLiteral(t.value(), t.line(), t.column());
        }
        String val = sb.toString();
        try { return new ASTNode.NumberLiteral(Double.parseDouble(val), line, col); }
        catch (NumberFormatException e) { return new ASTNode.StringLiteral(val, line, col); }
    }

    private ASTNode parseWhen() throws ScriptException {
        ModScriptLexer.Token whenToken = consume(ModScriptLexer.TokenType.WHEN);
        List<String> eventParts = new ArrayList<>();
        while (!peek().type().equals(ModScriptLexer.TokenType.COLON)) {
            skipNewlines();
            if (peek().type().equals(ModScriptLexer.TokenType.EOF)) break;
            eventParts.add(advance().value());
        }
        consume(ModScriptLexer.TokenType.COLON);
        skipNewlines();
        List<ASTNode> actions = parseActionBlock();
        return new ASTNode.WhenStatement(String.join(" ", eventParts), actions, whenToken.line(), whenToken.column());
    }

    private List<ASTNode> parseActionBlock() throws ScriptException {
        List<ASTNode> actions = new ArrayList<>();
        while (!isAtEnd()) {
            skipNewlines();
            if (isAtEnd()) break;
            if (!peek().type().equals(ModScriptLexer.TokenType.WORD) && !peek().type().equals(ModScriptLexer.TokenType.SET)
                    && !peek().type().equals(ModScriptLexer.TokenType.DEAL) && !peek().type().equals(ModScriptLexer.TokenType.PLAY)
                    && !peek().type().equals(ModScriptLexer.TokenType.GIVE) && !peek().type().equals(ModScriptLexer.TokenType.TELEPORT)
                    && !peek().type().equals(ModScriptLexer.TokenType.SPAWN) && !peek().type().equals(ModScriptLexer.TokenType.REMOVE)
                    && !peek().type().equals(ModScriptLexer.TokenType.APPLY) && !peek().type().equals(ModScriptLexer.TokenType.HEAL)
                    && !peek().type().equals(ModScriptLexer.TokenType.SUMMON) && !peek().type().equals(ModScriptLexer.TokenType.SHOOT)
                    && !peek().type().equals(ModScriptLexer.TokenType.CREATE)
                    && !peek().type().equals(ModScriptLexer.TokenType.WHEN)
                    && !peek().type().equals(ModScriptLexer.TokenType.HEALTH)
                    && !peek().type().equals(ModScriptLexer.TokenType.ATTACK)
                    && !peek().type().equals(ModScriptLexer.TokenType.SPEED)
                    && !peek().type().equals(ModScriptLexer.TokenType.DAMAGE)
                    && !peek().type().equals(ModScriptLexer.TokenType.FIRE)) break;
            actions.add(parseAction());
        }
        return actions;
    }

    private ASTNode parseAction() throws ScriptException {
        ModScriptLexer.Token token = peek();
        if (token.type().equals(ModScriptLexer.TokenType.GIVE)) { advance(); return parseGiveAction(token); }
        if (token.type().equals(ModScriptLexer.TokenType.TELEPORT)) { advance(); return parseTeleportAction(token); }
        if (token.type().equals(ModScriptLexer.TokenType.SPAWN)) { advance(); return parseSpawnAction(token); }
        if (token.type().equals(ModScriptLexer.TokenType.REMOVE)) { advance(); return parseRemoveAction(token); }
        if (token.type().equals(ModScriptLexer.TokenType.APPLY)) { advance(); return parseApplyAction(token); }
        if (token.type().equals(ModScriptLexer.TokenType.HEAL)) { advance(); return parseHealAction(token); }
        if (token.type().equals(ModScriptLexer.TokenType.SUMMON)) { advance(); return parseSummonAction(token); }
        if (token.type().equals(ModScriptLexer.TokenType.SHOOT)) { advance(); return parseShootAction(token); }
        if (token.type().equals(ModScriptLexer.TokenType.SET)) { advance(); return parseSetAction(); }
        if (token.type().equals(ModScriptLexer.TokenType.DEAL)) { advance(); return parseDealAction(); }
        if (token.type().equals(ModScriptLexer.TokenType.PLAY)) { advance(); return parsePlayAction(); }
        advance();
        return new ASTNode.ActionNode("unknown", List.of(), token.line(), token.column());
    }

    private ASTNode parseGiveAction(ModScriptLexer.Token token) throws ScriptException {
        ASTNode quantity = parseValue();
        ASTNode item = parseValue();
        return new ASTNode.ActionNode("give", List.of(quantity, item), token.line(), token.column());
    }

    private ASTNode parseTeleportAction(ModScriptLexer.Token token) throws ScriptException {
        List<ASTNode> params = new ArrayList<>();
        params.add(parseValue()); // x/y/z or target
        params.add(parseValue()); // y or x
        if (!isAtEnd() && (peek().type().equals(ModScriptLexer.TokenType.NUMBER) || peek().type().equals(ModScriptLexer.TokenType.WORD))) {
            params.add(parseValue()); // z
        }
        return new ASTNode.ActionNode("teleport", params, token.line(), token.column());
    }

    private ASTNode parseSpawnAction(ModScriptLexer.Token token) throws ScriptException {
        ASTNode mob = parseValue();
        return new ASTNode.ActionNode("spawn", List.of(mob), token.line(), token.column());
    }

    private ASTNode parseRemoveAction(ModScriptLexer.Token token) throws ScriptException {
        ASTNode target = parseValue();
        return new ASTNode.ActionNode("remove", List.of(target), token.line(), token.column());
    }

    private ASTNode parseApplyAction(ModScriptLexer.Token token) throws ScriptException {
        ASTNode effect = parseValue();
        ASTNode duration = null;
        if (!isAtEnd() && peek().type().equals(ModScriptLexer.TokenType.FOR)) { advance(); duration = parseValue(); }
        return new ASTNode.ActionNode("apply", List.of(effect, duration), token.line(), token.column());
    }

    private ASTNode parseHealAction(ModScriptLexer.Token token) throws ScriptException {
        ASTNode amount = parseValue();
        return new ASTNode.ActionNode("heal", List.of(amount), token.line(), token.column());
    }

    private ASTNode parseSummonAction(ModScriptLexer.Token token) throws ScriptException {
        ASTNode entity = parseValue();
        return new ASTNode.ActionNode("summon", List.of(entity), token.line(), token.column());
    }

    private ASTNode parseShootAction(ModScriptLexer.Token token) throws ScriptException {
        ASTNode projectile = parseValue();
        return new ASTNode.ActionNode("shoot", List.of(projectile), token.line(), token.column());
    }

    private ASTNode parseSetAction() throws ScriptException {
        List<ASTNode> params = new ArrayList<>();
        while (!peek().type().equals(ModScriptLexer.TokenType.NEWLINE) && !isAtEnd()) {
            params.add(parseValue());
        }
        return new ASTNode.ActionNode("set", params, 0, 0);
    }

    private ASTNode parseDealAction() throws ScriptException {
        ASTNode amount = parseValue();
        ASTNode type = null;
        if (peek().type().equals(ModScriptLexer.TokenType.WORD) && peek().value().equals("damage")) { advance(); }
        return new ASTNode.ActionNode("deal", List.of(amount), 0, 0);
    }

    private ASTNode parsePlayAction() throws ScriptException {
        ASTNode sound = parseValue();
        return new ASTNode.ActionNode("play", List.of(sound), 0, 0);
    }

    private String consumeString() throws ScriptException {
        ModScriptLexer.Token t = peek();
        if (t.type().equals(ModScriptLexer.TokenType.STRING)) {
            advance();
            return t.value();
        }
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && !peek().type().equals(ModScriptLexer.TokenType.COLON)
                && !peek().type().equals(ModScriptLexer.TokenType.NEWLINE)
                && !peek().type().equals(ModScriptLexer.TokenType.EOF)) {
            ModScriptLexer.Token word = advance();
            if (sb.length() > 0 && !word.value().equals(":")) sb.append(" ");
            sb.append(word.value());
        }
        if (sb.length() == 0) throw error("Expected string or name", t);
        return sb.toString();
    }

    private void skipNewlines() { while (!isAtEnd() && peek().type().equals(ModScriptLexer.TokenType.NEWLINE)) advance(); }

    private ModScriptLexer.Token advance() { ModScriptLexer.Token t = peek(); if (!isAtEnd()) position++; return t; }

    private ModScriptLexer.Token consume(ModScriptLexer.TokenType type) throws ScriptException {
        ModScriptLexer.Token t = peek();
        if (!t.type().equals(type)) throw error("Expected " + type, t);
        return advance();
    }

    private ModScriptLexer.Token peek() { return tokens.get(position); }
    private boolean isAtEnd() { return position >= tokens.size() || peek().type().equals(ModScriptLexer.TokenType.EOF); }
    private ScriptException error(String message, ModScriptLexer.Token token) { return new ScriptException(message, token.line(), token.column()); }

    public static class ScriptException extends Exception {
        private final int line, column;
        public ScriptException(String message, int line, int column) { super("Line " + line + ", Col " + column + ": " + message); this.line = line; this.column = column; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
    }
}
