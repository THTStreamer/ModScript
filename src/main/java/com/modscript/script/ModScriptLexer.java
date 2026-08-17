package com.modscript.script;

import java.util.ArrayList;
import java.util.List;

public class ModScriptLexer {
    private final String source;
    private int position;
    private int line;
    private int column;

    public ModScriptLexer(String source) {
        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (position < source.length()) {
            char c = current();

            if (c == '\n') {
                tokens.add(new Token(TokenType.NEWLINE, "\\n", line, column));
                advance();
                line++;
                column = 1;
            } else if (c == ' ' || c == '\t') {
                advance();
            } else if (c == '"') {
                tokens.add(readString());
            } else if (c == ':' && peek() == ':') {
                tokens.add(new Token(TokenType.DOUBLE_COLON, "::", line, column));
                advance(); advance();
            } else if (c == ':') {
                tokens.add(new Token(TokenType.COLON, ":", line, column));
                advance();
            } else if (c == '(') {
                tokens.add(new Token(TokenType.LEFT_PAREN, "(", line, column));
                advance();
            } else if (c == ')') {
                tokens.add(new Token(TokenType.RIGHT_PAREN, ")", line, column));
                advance();
            } else if (c == '[') {
                tokens.add(new Token(TokenType.LEFT_BRACKET, "[", line, column));
                advance();
            } else if (c == ']') {
                tokens.add(new Token(TokenType.RIGHT_BRACKET, "]", line, column));
                advance();
            } else if (c == '{') {
                tokens.add(new Token(TokenType.LEFT_BRACE, "{", line, column));
                advance();
            } else if (c == '}') {
                tokens.add(new Token(TokenType.RIGHT_BRACE, "}", line, column));
                advance();
            } else if (c == ',') {
                tokens.add(new Token(TokenType.COMMA, ",", line, column));
                advance();
            } else if (c == '-' && peek() == '>') {
                tokens.add(new Token(TokenType.ARROW, "->", line, column));
                advance(); advance();
            } else if (c == '-' && Character.isDigit(peekNext())) {
                tokens.add(readNumber());
            } else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            } else if (Character.isLetter(c) || c == '_') {
                tokens.add(readWord());
            } else {
                tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(c), line, column));
                advance();
            }
        }

        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private Token readString() {
        int startLine = line, startCol = column;
        StringBuilder sb = new StringBuilder();
        advance();
        while (position < source.length() && current() != '"') {
            if (current() == '\\') { advance(); if (position < source.length()) sb.append(current()); }
            else sb.append(current());
            advance();
        }
        if (position < source.length()) advance();
        return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
    }

    private Token readNumber() {
        int startLine = line, startCol = column;
        StringBuilder sb = new StringBuilder();
        if (current() == '-') { sb.append(current()); advance(); }
        while (position < source.length() && (Character.isDigit(current()) || current() == '.')) {
            sb.append(current()); advance();
        }
        return new Token(TokenType.NUMBER, sb.toString(), startLine, startCol);
    }

    private Token readWord() {
        int startLine = line, startCol = column;
        StringBuilder sb = new StringBuilder();
        while (position < source.length() && (Character.isLetterOrDigit(current()) || current() == '_')) {
            sb.append(current()); advance();
        }
        String word = sb.toString();

        return switch (word.toLowerCase()) {
            case "create" -> new Token(TokenType.CREATE, word, startLine, startCol);
            case "item" -> new Token(TokenType.ITEM, word, startLine, startCol);
            case "block" -> new Token(TokenType.BLOCK, word, startLine, startCol);
            case "mob" -> new Token(TokenType.MOB, word, startLine, startCol);
            case "effect" -> new Token(TokenType.EFFECT, word, startLine, startCol);
            case "recipe" -> new Token(TokenType.RECIPE, word, startLine, startCol);
            case "ability" -> new Token(TokenType.ABILITY, word, startLine, startCol);
            case "spell" -> new Token(TokenType.SPELL, word, startLine, startCol);
            case "particle" -> new Token(TokenType.PARTICLE, word, startLine, startCol);
            case "when" -> new Token(TokenType.WHEN, word, startLine, startCol);
            case "give" -> new Token(TokenType.GIVE, word, startLine, startCol);
            case "teleport" -> new Token(TokenType.TELEPORT, word, startLine, startCol);
            case "spawn" -> new Token(TokenType.SPAWN, word, startLine, startCol);
            case "remove" -> new Token(TokenType.REMOVE, word, startLine, startCol);
            case "apply" -> new Token(TokenType.APPLY, word, startLine, startCol);
            case "heal" -> new Token(TokenType.HEAL, word, startLine, startCol);
            case "summon" -> new Token(TokenType.SUMMON, word, startLine, startCol);
            case "shoot" -> new Token(TokenType.SHOOT, word, startLine, startCol);
            case "this" -> new Token(TokenType.THIS, word, startLine, startCol);
            case "is" -> new Token(TokenType.IS, word, startLine, startCol);
            case "hits" -> new Token(TokenType.HITS, word, startLine, startCol);
            case "set" -> new Token(TokenType.SET, word, startLine, startCol);
            case "the" -> new Token(TokenType.THE, word, startLine, startCol);
            case "on" -> new Token(TokenType.ON, word, startLine, startCol);
            case "fire" -> new Token(TokenType.FIRE, word, startLine, startCol);
            case "for" -> new Token(TokenType.FOR, word, startLine, startCol);
            case "seconds" -> new Token(TokenType.SECONDS, word, startLine, startCol);
            case "deal" -> new Token(TokenType.DEAL, word, startLine, startCol);
            case "damage" -> new Token(TokenType.DAMAGE, word, startLine, startCol);
            case "play" -> new Token(TokenType.PLAY, word, startLine, startCol);
            case "sound" -> new Token(TokenType.SOUND, word, startLine, startCol);
            case "particles" -> new Token(TokenType.PARTICLES, word, startLine, startCol);
            case "extra" -> new Token(TokenType.EXTRA, word, startLine, startCol);
            case "health" -> new Token(TokenType.HEALTH, word, startLine, startCol);
            case "attack" -> new Token(TokenType.ATTACK, word, startLine, startCol);
            case "speed" -> new Token(TokenType.SPEED, word, startLine, startCol);
            case "yes" -> new Token(TokenType.BOOLEAN_TRUE, word, startLine, startCol);
            case "no" -> new Token(TokenType.BOOLEAN_FALSE, word, startLine, startCol);
            case "true" -> new Token(TokenType.BOOLEAN_TRUE, word, startLine, startCol);
            case "false" -> new Token(TokenType.BOOLEAN_FALSE, word, startLine, startCol);
            default -> new Token(TokenType.WORD, word, startLine, startCol);
        };
    }

    private char current() { return source.charAt(position); }
    private char peek() { return position + 1 >= source.length() ? '\0' : source.charAt(position + 1); }
    private char peekNext() { return position + 2 >= source.length() ? '\0' : source.charAt(position + 2); }
    private void advance() { position++; column++; }

    public enum TokenType {
        STRING, NUMBER, BOOLEAN_TRUE, BOOLEAN_FALSE, WORD,
        CREATE, ITEM, BLOCK, MOB, EFFECT, RECIPE, ABILITY, SPELL, PARTICLE,
        WHEN, THIS, IS, HITS, SET, THE, ON, FIRE, FOR, SECONDS,
        DEAL, DAMAGE, PLAY, SOUND, PARTICLES, EXTRA,
        GIVE, TELEPORT, SPAWN, REMOVE, APPLY, HEAL, SUMMON, SHOOT,
        HEALTH, ATTACK, SPEED,
        COLON, DOUBLE_COLON, LEFT_PAREN, RIGHT_PAREN,
        LEFT_BRACKET, RIGHT_BRACKET, LEFT_BRACE, RIGHT_BRACE,
        COMMA, ARROW, NEWLINE, EOF, UNKNOWN
    }

    public record Token(TokenType type, String value, int line, int column) {
        @Override
        public String toString() { return type + "(" + value + ")@" + line + ":" + column; }
    }
}
