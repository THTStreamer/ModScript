package com.modscript.gui;

import com.modscript.script.ModScriptLexer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class CodeEditorScreen extends Screen {
    private final String projectName;
    private final List<String> lines = new ArrayList<>();
    private int cursorLine = 0;
    private int cursorColumn = 0;
    private int scrollOffset = 0;
    private int maxVisibleLines;
    private boolean cursorVisible = true;
    private long lastCursorBlink = 0;

    private List<String> autocompleteSuggestions = new ArrayList<>();
    private int selectedSuggestion = 0;
    private boolean showAutocomplete = false;
    private String contextHint = null;

    private List<String> errors = new ArrayList<>();
    private String statusMessage = "";
    private long statusMessageTime = 0;

    private boolean showSearch = false;
    private String searchText = "";
    private int searchCursorPos = 0;
    private List<int[]> searchResults = new ArrayList<>();
    private int currentSearchResult = 0;

    // Layout
    private static final int TITLE_BAR_HEIGHT = 20;
    private static final int TOOLBAR_HEIGHT = 24;
    private static final int HEADER_HEIGHT = TITLE_BAR_HEIGHT + TOOLBAR_HEIGHT; // 44
    private static final int STATUS_BAR_HEIGHT = 20;
    private static final int LINE_HEIGHT = 12;
    private static final int LINE_NUM_WIDTH = 35;
    private static final int EDITOR_LEFT = LINE_NUM_WIDTH + 8;
    private static final int AUTOCOMPLETE_WIDTH = 200;
    private static final int AUTOCOMPLETE_ITEM_HEIGHT = 13;

    // Button layout
    private static final int BTN_Y_OFFSET = 3;
    private static final int BTN_HEIGHT = 18;
    private static final int BTN_SAVE_X = 8;
    private static final int BTN_SAVE_W = 52;
    private static final int BTN_RUN_X = 66;
    private static final int BTN_RUN_W = 52;

    public CodeEditorScreen(String projectName) {
        super(Component.literal("ModScript Editor - " + projectName));
        this.projectName = projectName;
        this.lines.add("create item \"Dragon Sword\"");
        this.lines.add("damage: 25");
        this.lines.add("durability: 1500");
        this.lines.add("");
        this.lines.add("when player attacks zombie:");
        this.lines.add("    set zombie on fire for 5 seconds");
        this.lines.add("    deal 20 damage");
    }

    @Override
    protected void init() {
        maxVisibleLines = (this.height - HEADER_HEIGHT - STATUS_BAR_HEIGHT - 4) / LINE_HEIGHT;
        if (maxVisibleLines < 1) maxVisibleLines = 1;
        validateScript();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int editorTop = HEADER_HEIGHT;
        int editorBottom = this.height - STATUS_BAR_HEIGHT;

        // === TITLE BAR ===
        guiGraphics.fill(0, 0, this.width, TITLE_BAR_HEIGHT, 0xFF1A1A2E);
        guiGraphics.drawCenteredString(this.font, "ModScript Editor - " + projectName, this.width / 2, 6, 0xFFCCCCCC);

        // === TOOLBAR ===
        guiGraphics.fill(0, TITLE_BAR_HEIGHT, this.width, HEADER_HEIGHT, 0xFF252535);

        // Save button
        int btnY = TITLE_BAR_HEIGHT + BTN_Y_OFFSET;
        boolean saveHover = mouseX >= BTN_SAVE_X && mouseX <= BTN_SAVE_X + BTN_SAVE_W
                && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT;
        int saveBg = saveHover ? 0xFF2E6B2E : 0xFF1E4E1E;
        int saveBorder = saveHover ? 0xFF44AA44 : 0xFF338833;
        guiGraphics.fill(BTN_SAVE_X, btnY, BTN_SAVE_X + BTN_SAVE_W, btnY + BTN_HEIGHT, saveBg);
        guiGraphics.renderOutline(BTN_SAVE_X, btnY, BTN_SAVE_W, BTN_HEIGHT, saveBorder);
        guiGraphics.drawCenteredString(this.font, "Save", BTN_SAVE_X + BTN_SAVE_W / 2, btnY + 5, 0xFF66DD66);

        // Run button
        boolean runHover = mouseX >= BTN_RUN_X && mouseX <= BTN_RUN_X + BTN_RUN_W
                && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT;
        int runBg = runHover ? 0xFF6B2E2E : 0xFF4E1E1E;
        int runBorder = runHover ? 0xFFAA4444 : 0xFF883333;
        guiGraphics.fill(BTN_RUN_X, btnY, BTN_RUN_X + BTN_RUN_W, btnY + BTN_HEIGHT, runBg);
        guiGraphics.renderOutline(BTN_RUN_X, btnY, BTN_RUN_W, BTN_HEIGHT, runBorder);
        guiGraphics.drawCenteredString(this.font, "Run", BTN_RUN_X + BTN_RUN_W / 2, btnY + 5, 0xFFDD6666);

        // Project label
        guiGraphics.drawString(this.font, "  " + projectName, BTN_RUN_X + BTN_RUN_W + 10, btnY + 5, 0xFF888888);

        // Separator under toolbar
        guiGraphics.fill(0, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, 0xFF444444);

        // === EDITOR BACKGROUND ===
        guiGraphics.fill(0, editorTop, this.width, editorBottom, 0xFF1E1E1E);

        // === LINE NUMBER COLUMN BACKGROUND ===
        guiGraphics.fill(0, editorTop, LINE_NUM_WIDTH, editorBottom, 0xFF1A1A1A);
        guiGraphics.fill(LINE_NUM_WIDTH, editorTop, LINE_NUM_WIDTH + 1, editorBottom, 0xFF333333);

        // === CURRENT LINE HIGHLIGHT ===
        if (cursorLine >= scrollOffset && cursorLine < scrollOffset + maxVisibleLines) {
            int hlY = editorTop + (cursorLine - scrollOffset) * LINE_HEIGHT;
            guiGraphics.fill(LINE_NUM_WIDTH + 1, hlY, this.width, hlY + LINE_HEIGHT, 0xFF2A2D2E);
        }

        // === LINE NUMBERS ===
        for (int i = 0; i < maxVisibleLines && i + scrollOffset < lines.size(); i++) {
            int y = editorTop + i * LINE_HEIGHT + 2;
            int lineNum = i + scrollOffset + 1;
            String numStr = String.valueOf(lineNum);
            int numWidth = font.width(numStr);
            int numColor = (i + scrollOffset == cursorLine) ? 0xFFBBBBBB : 0xFF555555;
            guiGraphics.drawString(this.font, numStr, LINE_NUM_WIDTH - numWidth - 6, y, numColor);
        }

        // === CODE ===
        for (int i = 0; i < maxVisibleLines && i + scrollOffset < lines.size(); i++) {
            int y = editorTop + i * LINE_HEIGHT + 2;
            String line = lines.get(i + scrollOffset);
            renderHighlightedLine(guiGraphics, line, EDITOR_LEFT, y);
        }

        // === SEARCH RESULTS HIGHLIGHTING ===
        if (showSearch && !searchResults.isEmpty()) {
            for (int[] result : searchResults) {
                int lineIdx = result[0];
                int colIdx = result[1];
                if (lineIdx >= scrollOffset && lineIdx < scrollOffset + maxVisibleLines) {
                    int y = editorTop + (lineIdx - scrollOffset) * LINE_HEIGHT;
                    String line = lines.get(lineIdx);
                    int x = EDITOR_LEFT + font.width(line.substring(0, colIdx));
                    int w = font.width(searchText);
                    guiGraphics.fill(x, y, x + w, y + LINE_HEIGHT, 0x66FFFF00);
                }
            }
        }

        // === CURSOR ===
        long now = System.currentTimeMillis();
        if (now - lastCursorBlink > 530) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = now;
        }

        if (cursorVisible && cursorLine >= scrollOffset && cursorLine < scrollOffset + maxVisibleLines) {
            int cursorY = editorTop + (cursorLine - scrollOffset) * LINE_HEIGHT + 2;
            String line = lines.get(cursorLine);
            int clampedCol = Math.min(cursorColumn, line.length());
            int cursorX = EDITOR_LEFT + font.width(line.substring(0, clampedCol));
            guiGraphics.fill(cursorX, cursorY, cursorX + 1, cursorY + LINE_HEIGHT - 2, 0xFFCCCCCC);
        }

        // === AUTOCOMPLETE POPUP ===
        if (showAutocomplete && !autocompleteSuggestions.isEmpty()) {
            renderAutocomplete(guiGraphics);
        }

        // === ERROR PANEL ===
        if (!errors.isEmpty()) {
            renderErrors(guiGraphics, editorTop, editorBottom);
        }

        // === SEARCH BAR ===
        if (showSearch) {
            renderSearchBar(guiGraphics);
        }

        // === STATUS BAR ===
        guiGraphics.fill(0, editorBottom, this.width, this.height, 0xFF252526);
        guiGraphics.fill(0, editorBottom, this.width, editorBottom + 1, 0xFF444444);

        String leftStatus = "Ln " + (cursorLine + 1) + "  Col " + (cursorColumn + 1) + "  |  " + lines.size() + " lines";
        if (!errors.isEmpty()) leftStatus += "  |  " + errors.size() + " error(s)";
        guiGraphics.drawString(this.font, leftStatus, 8, editorBottom + 6, errors.isEmpty() ? 0xFF888888 : 0xFFFF5555);

        String rightStatus = "Ctrl+S Save  |  F5 Run  |  Ctrl+F Find";
        guiGraphics.drawString(this.font, rightStatus, this.width - font.width(rightStatus) - 8, editorBottom + 6, 0xFF666666);

        // === STATUS MESSAGE TOAST ===
        if (!statusMessage.isEmpty() && now - statusMessageTime < 3000) {
            int msgW = font.width(statusMessage) + 20;
            int msgX = (this.width - msgW) / 2;
            int msgY = editorBottom - 22;
            guiGraphics.fill(msgX - 5, msgY - 2, msgX + msgW + 5, msgY + 14, 0xCC1A3A1A);
            guiGraphics.renderOutline(msgX - 5, msgY - 2, msgW + 10, 16, 0xFF338833);
            guiGraphics.drawString(this.font, statusMessage, msgX, msgY + 1, 0xFF55FF55);
        }
    }

    private void renderHighlightedLine(GuiGraphics guiGraphics, String line, int x, int y) {
        int offsetX = 0;
        String trimmed = line.trim();

        if (trimmed.startsWith("create")) {
            int createEnd = line.indexOf(' ') + 1;
            guiGraphics.drawString(this.font, line.substring(0, createEnd), x + offsetX, y, 0xFF569CD6);
            offsetX += font.width(line.substring(0, createEnd));

            String remaining = line.substring(createEnd);
            String trimmedRemaining = remaining.trim();
            if (trimmedRemaining.startsWith("item") || trimmedRemaining.startsWith("block")) {
                int spaces = remaining.indexOf(trimmedRemaining);
                if (spaces > 0) {
                    guiGraphics.drawString(this.font, remaining.substring(0, spaces), x + offsetX, y, 0xFFD4D4D4);
                    offsetX += font.width(remaining.substring(0, spaces));
                }
                int typeEnd = trimmedRemaining.indexOf(' ') + 1;
                if (typeEnd <= 0) typeEnd = trimmedRemaining.length();
                guiGraphics.drawString(this.font, trimmedRemaining.substring(0, typeEnd), x + offsetX, y, 0xFF4EC9B0);
                offsetX += font.width(trimmedRemaining.substring(0, typeEnd));
                remaining = trimmedRemaining.substring(typeEnd);
            } else {
                remaining = trimmedRemaining;
            }

            if (remaining.contains("\"")) {
                int firstQuote = remaining.indexOf('"');
                int lastQuote = remaining.lastIndexOf('"');
                if (firstQuote != lastQuote) {
                    guiGraphics.drawString(this.font, remaining.substring(0, firstQuote + 1), x + offsetX, y, 0xFFD4D4D4);
                    offsetX += font.width(remaining.substring(0, firstQuote + 1));
                    guiGraphics.drawString(this.font, remaining.substring(firstQuote + 1, lastQuote), x + offsetX, y, 0xFFCE9178);
                    offsetX += font.width(remaining.substring(firstQuote + 1, lastQuote));
                    guiGraphics.drawString(this.font, remaining.substring(lastQuote), x + offsetX, y, 0xFFD4D4D4);
                    return;
                }
            }
            guiGraphics.drawString(this.font, remaining, x + offsetX, y, 0xFFD4D4D4);

        } else if (trimmed.startsWith("when")) {
            guiGraphics.drawString(this.font, line, x, y, 0xFFC586C0);

        } else if (trimmed.contains(":")) {
            int colonPos = line.indexOf(':');
            String key = line.substring(0, colonPos + 1);
            String val = line.substring(colonPos + 1);
            guiGraphics.drawString(this.font, key, x, y, 0xFF9CDCFE);
            guiGraphics.drawString(this.font, val, x + font.width(key), y, 0xFFB5CEA8);

        } else if (line.contains("\"")) {
            int firstQuote = line.indexOf('"');
            int lastQuote = line.lastIndexOf('"');
            if (firstQuote != lastQuote) {
                guiGraphics.drawString(this.font, line.substring(0, firstQuote + 1), x + offsetX, y, 0xFFD4D4D4);
                offsetX += font.width(line.substring(0, firstQuote + 1));
                guiGraphics.drawString(this.font, line.substring(firstQuote + 1, lastQuote), x + offsetX, y, 0xFFCE9178);
                offsetX += font.width(line.substring(firstQuote + 1, lastQuote));
                guiGraphics.drawString(this.font, line.substring(lastQuote), x + offsetX, y, 0xFFD4D4D4);
                return;
            }
            guiGraphics.drawString(this.font, line, x, y, 0xFFD4D4D4);

        } else {
            guiGraphics.drawString(this.font, line, x, y, 0xFFD4D4D4);
        }
    }

    private void renderAutocomplete(GuiGraphics guiGraphics) {
        String currentLine = lines.get(cursorLine);
        int clampedCol = Math.min(cursorColumn, currentLine.length());
        int x = EDITOR_LEFT + font.width(currentLine.substring(0, clampedCol));
        int y = HEADER_HEIGHT + (cursorLine - scrollOffset) * LINE_HEIGHT + LINE_HEIGHT + 2;

        int popupHeight = autocompleteSuggestions.size() * AUTOCOMPLETE_ITEM_HEIGHT + 4;
        if (y + popupHeight > this.height - STATUS_BAR_HEIGHT - 10) {
            y = HEADER_HEIGHT + (cursorLine - scrollOffset) * LINE_HEIGHT - popupHeight - 2;
        }

        guiGraphics.fill(x, y, x + AUTOCOMPLETE_WIDTH, y + popupHeight, 0xFF252526);
        guiGraphics.renderOutline(x, y, AUTOCOMPLETE_WIDTH, popupHeight, 0xFF555555);

        for (int i = 0; i < autocompleteSuggestions.size(); i++) {
            int itemY = y + 2 + i * AUTOCOMPLETE_ITEM_HEIGHT;
            if (i == selectedSuggestion) {
                guiGraphics.fill(x + 1, itemY, x + AUTOCOMPLETE_WIDTH - 1, itemY + AUTOCOMPLETE_ITEM_HEIGHT - 1, 0xFF264F78);
            }
            guiGraphics.drawString(this.font, autocompleteSuggestions.get(i), x + 6, itemY + 3, 0xFFD4D4D4);
        }
    }

    private void renderErrors(GuiGraphics guiGraphics, int editorTop, int editorBottom) {
        int panelHeight = Math.min(errors.size(), 4) * 13 + 18;
        int panelY = editorBottom - panelHeight - 4;

        guiGraphics.fill(EDITOR_LEFT, panelY, this.width, editorBottom - 4, 0xDD3D1F1F);
        guiGraphics.renderOutline(EDITOR_LEFT, panelY, this.width - EDITOR_LEFT, panelHeight, 0xFFAA3333);
        guiGraphics.drawString(this.font, "Errors:", EDITOR_LEFT + 4, panelY + 3, 0xFFFF5555);

        for (int i = 0; i < Math.min(errors.size(), 4); i++) {
            guiGraphics.drawString(this.font, errors.get(i), EDITOR_LEFT + 4, panelY + 16 + i * 13, 0xFFFF9999);
        }
    }

    private void renderSearchBar(GuiGraphics guiGraphics) {
        int barY = HEADER_HEIGHT;
        guiGraphics.fill(0, barY, this.width, barY + 20, 0xFF2A2A3A);
        guiGraphics.fill(0, barY + 19, this.width, barY + 20, 0xFF444444);

        guiGraphics.drawString(this.font, "Find:", 8, barY + 6, 0xFF888888);
        guiGraphics.fill(48, barY + 3, 220, barY + 16, 0xFF1A1A1A);
        guiGraphics.renderOutline(48, barY + 3, 172, 13, 0xFF555555);
        String display = searchText + (System.currentTimeMillis() % 1000 < 500 ? "|" : "");
        guiGraphics.drawString(this.font, display, 52, barY + 5, 0xFFD4D4D4);

        if (!searchResults.isEmpty()) {
            guiGraphics.drawString(this.font,
                (currentSearchResult + 1) + "/" + searchResults.size(),
                230, barY + 6, 0xFF888888);
        }
    }

    private void validateScript() {
        errors.clear();
        try {
            ModScriptLexer lexer = new ModScriptLexer(String.join("\n", lines));
            lexer.tokenize();
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("create") && !line.contains("\"")) {
                errors.add("Line " + (i + 1) + ": Missing name in quotes");
            }
            if (line.startsWith("when") && !line.endsWith(":")) {
                errors.add("Line " + (i + 1) + ": 'when' must end with ':'");
            }
        }
    }

    private void updateAutocomplete() {
        String currentLine = lines.get(cursorLine);
        autocompleteSuggestions = AutocompleteEngine.getSuggestions(currentLine, cursorColumn);
        contextHint = AutocompleteEngine.getContextHint(currentLine);
        showAutocomplete = !autocompleteSuggestions.isEmpty();
        selectedSuggestion = 0;
    }

    private void performSearch() {
        searchResults.clear();
        if (searchText.isEmpty()) return;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).toLowerCase();
            String search = searchText.toLowerCase();
            int idx = 0;
            while ((idx = line.indexOf(search, idx)) != -1) {
                searchResults.add(new int[]{i, idx});
                idx++;
            }
        }
        if (!searchResults.isEmpty()) {
            currentSearchResult = 0;
            jumpToSearchResult();
        }
    }

    private void jumpToSearchResult() {
        if (searchResults.isEmpty()) return;
        int[] result = searchResults.get(currentSearchResult);
        cursorLine = result[0];
        cursorColumn = result[1];
        ensureCursorVisible();
    }

    private void saveScript() {
        String script = String.join("\n", lines);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand("modcreator save " + projectName + " " + escapeForCommand(script));
        }
        showStatus("Script saved!");
    }

    private void runScript() {
        if (!errors.isEmpty()) {
            showStatus("Fix errors first!");
            return;
        }
        String script = String.join("\n", lines);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand("modcreator run " + projectName + " " + escapeForCommand(script));
        }
        showStatus("Running...");
    }

    private String escapeForCommand(String s) {
        return s.replace("\n", "\\n").replace("\"", "\\\"");
    }

    private void showStatus(String message) {
        statusMessage = message;
        statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int btnY = TITLE_BAR_HEIGHT + BTN_Y_OFFSET;

        if (mouseX >= BTN_SAVE_X && mouseX <= BTN_SAVE_X + BTN_SAVE_W
                && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT) {
            saveScript();
            return true;
        }
        if (mouseX >= BTN_RUN_X && mouseX <= BTN_RUN_X + BTN_RUN_W
                && mouseY >= btnY && mouseY <= btnY + BTN_HEIGHT) {
            runScript();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 344) { runScript(); return true; }
        if (keyCode == 83 && hasControlDown()) { saveScript(); return true; }
        if (keyCode == 70 && hasControlDown()) {
            showSearch = !showSearch;
            if (showSearch) { searchText = ""; searchCursorPos = 0; }
            return true;
        }
        if (keyCode == 32 && hasControlDown()) { updateAutocomplete(); return true; }

        if (showAutocomplete && keyCode == 257) {
            if (!autocompleteSuggestions.isEmpty()) insertAutocomplete(autocompleteSuggestions.get(selectedSuggestion));
            showAutocomplete = false;
            return true;
        }
        if (showAutocomplete) {
            if (keyCode == 265) { selectedSuggestion = Math.max(0, selectedSuggestion - 1); return true; }
            if (keyCode == 264) { selectedSuggestion = Math.min(autocompleteSuggestions.size() - 1, selectedSuggestion + 1); return true; }
            if (keyCode == 256) { showAutocomplete = false; return true; }
        }

        if (showSearch) {
            if (keyCode == 256) { showSearch = false; return true; }
            if (keyCode == 257 && !searchResults.isEmpty()) {
                currentSearchResult = (currentSearchResult + 1) % searchResults.size();
                jumpToSearchResult();
                return true;
            }
            if (keyCode == 259 && searchCursorPos > 0) {
                searchText = searchText.substring(0, searchCursorPos - 1) + searchText.substring(searchCursorPos);
                searchCursorPos--;
                performSearch();
                return true;
            }
        }

        if (keyCode == 265) { if (cursorLine > 0) cursorLine--; cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length()); ensureCursorVisible(); validateScript(); return true; }
        if (keyCode == 264) { if (cursorLine < lines.size() - 1) cursorLine++; cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length()); ensureCursorVisible(); validateScript(); return true; }
        if (keyCode == 263) { if (cursorColumn > 0) cursorColumn--; else if (cursorLine > 0) { cursorLine--; cursorColumn = lines.get(cursorLine).length(); } return true; }
        if (keyCode == 262) { if (cursorColumn < lines.get(cursorLine).length()) cursorColumn++; else if (cursorLine < lines.size() - 1) { cursorLine++; cursorColumn = 0; } return true; }
        if (keyCode == 335 || keyCode == 331) { cursorColumn = 0; return true; }
        if (keyCode == 336 || keyCode == 332) { cursorColumn = lines.get(cursorLine).length(); return true; }

        if (keyCode == 257) {
            String before = lines.get(cursorLine).substring(0, cursorColumn);
            String after = lines.get(cursorLine).substring(cursorColumn);
            int indent = 0;
            while (indent < before.length() && before.charAt(indent) == ' ') indent++;
            lines.set(cursorLine, before);
            lines.add(cursorLine + 1, before.substring(0, indent) + after);
            cursorLine++;
            cursorColumn = indent;
            ensureCursorVisible();
            validateScript();
            return true;
        }

        if (keyCode == 259) {
            if (cursorColumn > 0) {
                String line = lines.get(cursorLine);
                lines.set(cursorLine, line.substring(0, cursorColumn - 1) + line.substring(cursorColumn));
                cursorColumn--;
            } else if (cursorLine > 0) {
                cursorColumn = lines.get(cursorLine - 1).length();
                lines.set(cursorLine - 1, lines.get(cursorLine - 1) + lines.get(cursorLine));
                lines.remove(cursorLine);
                cursorLine--;
            }
            validateScript();
            return true;
        }

        if (keyCode == 261) {
            String line = lines.get(cursorLine);
            if (cursorColumn < line.length()) {
                lines.set(cursorLine, line.substring(0, cursorColumn) + line.substring(cursorColumn + 1));
            } else if (cursorLine < lines.size() - 1) {
                lines.set(cursorLine, line + lines.get(cursorLine + 1));
                lines.remove(cursorLine + 1);
            }
            validateScript();
            return true;
        }

        if (keyCode == 258) {
            String line = lines.get(cursorLine);
            lines.set(cursorLine, line.substring(0, cursorColumn) + "    " + line.substring(cursorColumn));
            cursorColumn += 4;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showSearch) {
            if (codePoint >= 32 && codePoint < 127) {
                searchText = searchText.substring(0, searchCursorPos) + codePoint + searchText.substring(searchCursorPos);
                searchCursorPos++;
                performSearch();
                return true;
            }
        } else if (codePoint >= 32 && codePoint < 127) {
            String line = lines.get(cursorLine);
            lines.set(cursorLine, line.substring(0, cursorColumn) + codePoint + line.substring(cursorColumn));
            cursorColumn++;
            showAutocomplete = false;
            validateScript();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void insertAutocomplete(String suggestion) {
        String line = lines.get(cursorLine);
        lines.set(cursorLine, line.substring(0, cursorColumn) + suggestion + line.substring(cursorColumn));
        cursorColumn += suggestion.length();
        validateScript();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Mth.clamp(scrollOffset - (int) verticalAmount, 0, Math.max(0, lines.size() - maxVisibleLines));
        return true;
    }

    private void ensureCursorVisible() {
        if (cursorLine < scrollOffset) scrollOffset = cursorLine;
        else if (cursorLine >= scrollOffset + maxVisibleLines) scrollOffset = cursorLine - maxVisibleLines + 1;
    }

    public String getScript() { return String.join("\n", lines); }

    @Override
    public boolean isPauseScreen() { return true; }
}
