package com.modscript.gui;

import com.modscript.project.ProjectManager;
import com.modscript.script.ModScriptLexer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class CodeEditorScreen extends Screen {
    private String projectName;
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

    private List<String> projectList = new ArrayList<>();
    private boolean showProjectList = false;

    // Layout
    private static final int TITLE_BAR_HEIGHT = 20;
    private static final int TOOLBAR_HEIGHT = 24;
    private static final int HEADER_HEIGHT = TITLE_BAR_HEIGHT + TOOLBAR_HEIGHT;
    private static final int STATUS_BAR_HEIGHT = 20;
    private static final int LINE_HEIGHT = 12;
    private static final int LINE_NUM_WIDTH = 35;
    private static final int EDITOR_LEFT = LINE_NUM_WIDTH + 8;

    private static final int BTN_Y = TITLE_BAR_HEIGHT + 3;
    private static final int BTN_H = 18;

    public CodeEditorScreen(String projectName) {
        super(Component.literal("ModScript Editor"));
        this.projectName = projectName;
        loadProjectScript();
    }

    private void loadProjectScript() {
        lines.clear();
        try {
            String script = ProjectManager.loadScript(projectName);
            if (script != null && !script.isEmpty()) {
                for (String line : script.split("\n")) lines.add(line);
            }
        } catch (Exception e) {}
        if (lines.isEmpty()) {
            lines.add("create item \"Dragon Sword\"");
            lines.add("damage: 25");
            lines.add("durability: 1500");
        }
        cursorLine = 0;
        cursorColumn = 0;
        scrollOffset = 0;
        validateScript();
    }

    private void refreshProjectList() {
        projectList.clear();
        try { projectList.addAll(ProjectManager.listProjects()); } catch (Exception e) {}
    }

    @Override
    protected void init() {
        maxVisibleLines = (this.height - HEADER_HEIGHT - STATUS_BAR_HEIGHT - 4) / LINE_HEIGHT;
        if (maxVisibleLines < 1) maxVisibleLines = 1;
        refreshProjectList();
        validateScript();
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        if (this.font == null) return;
        gui.fill(0, 0, this.width, this.height, 0xFF1A1A2E);

        int editorTop = HEADER_HEIGHT;
        int editorBottom = this.height - STATUS_BAR_HEIGHT;

        // Title bar
        gui.fill(0, 0, this.width, TITLE_BAR_HEIGHT, 0xFF1A1A2E);
        gui.drawCenteredString(this.font, "ModScript - " + projectName, this.width / 2, 6, 0xFFCCCCCC);

        // Toolbar
        gui.fill(0, TITLE_BAR_HEIGHT, this.width, HEADER_HEIGHT, 0xFF252535);

        int x = 8;

        // Projects button
        int projW = font.width("Projects") + 12;
        boolean projHover = mx >= x && mx <= x + projW && my >= BTN_Y && my <= BTN_Y + BTN_H;
        gui.fill(x, BTN_Y, x + projW, BTN_Y + BTN_H, showProjectList ? 0xFF4A6FA5 : (projHover ? 0xFF3A3A4A : 0xFF2A2A3A));
        gui.drawString(font, "Projects", x + 6, BTN_Y + 5, 0xFFCCCCCC);
        x += projW + 6;

        // New button
        int newW = font.width("New") + 10;
        boolean newHover = mx >= x && mx <= x + newW && my >= BTN_Y && my <= BTN_Y + BTN_H;
        gui.fill(x, BTN_Y, x + newW, BTN_Y + BTN_H, newHover ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.drawString(font, "New", x + 5, BTN_Y + 5, 0xFF88CCFF);
        x += newW + 6;

        // Separator
        gui.fill(x, BTN_Y, x + 1, BTN_Y + BTN_H, 0xFF444444);
        x += 8;

        // Save button
        int saveW = font.width("Save") + 12;
        boolean saveHover = mx >= x && mx <= x + saveW && my >= BTN_Y && my <= BTN_Y + BTN_H;
        gui.fill(x, BTN_Y, x + saveW, BTN_Y + BTN_H, saveHover ? 0xFF2E6B2E : 0xFF1E4E1E);
        gui.drawString(font, "Save", x + 6, BTN_Y + 5, 0xFF66DD66);
        x += saveW + 4;

        // Run button
        int runW = font.width("Run") + 12;
        boolean runHover = mx >= x && mx <= x + runW && my >= BTN_Y && my <= BTN_Y + BTN_H;
        gui.fill(x, BTN_Y, x + runW, BTN_Y + BTN_H, runHover ? 0xFF6B2E2E : 0xFF4E1E1E);
        gui.drawString(font, "Run", x + 6, BTN_Y + 5, 0xFFDD6666);
        x += runW + 12;

        // Undo/Redo
        int undoW = font.width("Undo") + 10;
        gui.fill(x, BTN_Y, x + undoW, BTN_Y + BTN_H, mx >= x && mx <= x + undoW && my >= BTN_Y && my <= BTN_Y + BTN_H ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.drawString(font, "Undo", x + 5, BTN_Y + 5, 0xFFDDDDDD);
        x += undoW + 4;

        int redoW = font.width("Redo") + 10;
        gui.fill(x, BTN_Y, x + redoW, BTN_Y + BTN_H, mx >= x && mx <= x + redoW && my >= BTN_Y && my <= BTN_Y + BTN_H ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.drawString(font, "Redo", x + 5, BTN_Y + 5, 0xFFDDDDDD);
        x += redoW + 12;

        // Close
        int closeW = font.width("Close") + 10;
        gui.fill(x, BTN_Y, x + closeW, BTN_Y + BTN_H, mx >= x && mx <= x + closeW && my >= BTN_Y && my <= BTN_Y + BTN_H ? 0xFF6B2E2E : 0xFF4E1E1E);
        gui.drawString(font, "Close", x + 5, BTN_Y + 5, 0xFFDD6666);

        // Separator
        gui.fill(0, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, 0xFF444444);

        // Editor background
        gui.fill(0, editorTop, this.width, editorBottom, 0xFF1E1E1E);
        gui.fill(0, editorTop, LINE_NUM_WIDTH, editorBottom, 0xFF1A1A1A);
        gui.fill(LINE_NUM_WIDTH, editorTop, LINE_NUM_WIDTH + 1, editorBottom, 0xFF333333);

        // Current line highlight
        if (cursorLine >= scrollOffset && cursorLine < scrollOffset + maxVisibleLines) {
            int hlY = editorTop + (cursorLine - scrollOffset) * LINE_HEIGHT;
            gui.fill(LINE_NUM_WIDTH + 1, hlY, this.width, hlY + LINE_HEIGHT, 0xFF2A2D2E);
        }

        // Line numbers
        for (int i = 0; i < maxVisibleLines && i + scrollOffset < lines.size(); i++) {
            int y = editorTop + i * LINE_HEIGHT + 2;
            int num = i + scrollOffset + 1;
            String numStr = String.valueOf(num);
            int numW = font.width(numStr);
            int numColor = (i + scrollOffset == cursorLine) ? 0xFFBBBBBB : 0xFF555555;
            gui.drawString(this.font, numStr, LINE_NUM_WIDTH - numW - 6, y, numColor);
        }

        // Code
        for (int i = 0; i < maxVisibleLines && i + scrollOffset < lines.size(); i++) {
            int y = editorTop + i * LINE_HEIGHT + 2;
            String line = lines.get(i + scrollOffset);
            renderHighlightedLine(gui, line, EDITOR_LEFT, y);
        }

        // Cursor
        long now = System.currentTimeMillis();
        if (now - lastCursorBlink > 530) { cursorVisible = !cursorVisible; lastCursorBlink = now; }
        if (cursorVisible && cursorLine >= scrollOffset && cursorLine < scrollOffset + maxVisibleLines) {
            int cursorY = editorTop + (cursorLine - scrollOffset) * LINE_HEIGHT + 2;
            String line = lines.get(cursorLine);
            int clampedCol = Math.min(cursorColumn, line.length());
            int cursorX = EDITOR_LEFT + font.width(line.substring(0, clampedCol));
            gui.fill(cursorX, cursorY, cursorX + 1, cursorY + LINE_HEIGHT - 2, 0xFFCCCCCC);
        }

        // Error panel
        if (!errors.isEmpty()) {
            int panelH = Math.min(errors.size(), 4) * 13 + 18;
            int panelY = editorBottom - panelH - 4;
            gui.fill(EDITOR_LEFT, panelY, this.width, editorBottom - 4, 0xDD3D1F1F);
            gui.drawString(this.font, "Errors:", EDITOR_LEFT + 4, panelY + 3, 0xFFFF5555);
            for (int i = 0; i < Math.min(errors.size(), 4); i++) {
                gui.drawString(this.font, errors.get(i), EDITOR_LEFT + 4, panelY + 16 + i * 13, 0xFFFF9999);
            }
        }

        // Project list dropdown
        if (showProjectList) {
            renderProjectList(gui, mx, my);
        }

        // Status bar
        gui.fill(0, editorBottom, this.width, this.height, 0xFF252526);
        String left = "Ln " + (cursorLine + 1) + "  Col " + (cursorColumn + 1) + "  |  " + lines.size() + " lines";
        if (!errors.isEmpty()) left += "  |  " + errors.size() + " error(s)";
        gui.drawString(this.font, left, 8, editorBottom + 6, errors.isEmpty() ? 0xFF888888 : 0xFFFF5555);

        // Status message
        if (!statusMessage.isEmpty() && now - statusMessageTime < 3000) {
            int w = font.width(statusMessage) + 16;
            int sx = (this.width - w) / 2;
            gui.fill(sx, editorBottom - 22, sx + w, editorBottom - 6, 0xCC1A3A1A);
            gui.drawString(this.font, statusMessage, sx + 8, editorBottom - 18, 0xFF55FF55);
        }
    }

    private void renderProjectList(GuiGraphics gui, int mx, int my) {
        int listX = 8;
        int listY = HEADER_HEIGHT;
        int itemH = 14;
        int listW = 150;
        int listH = (projectList.size() + 1) * itemH + 4;

        gui.fill(listX, listY, listX + listW, listY + listH, 0xFF252535);
        gui.renderOutline(listX, listY, listW, listH, 0xFF555555);

        for (int i = 0; i < projectList.size(); i++) {
            int iy = listY + 2 + i * itemH;
            boolean hover = mx >= listX && mx <= listX + listW && my >= iy && my <= iy + itemH;
            boolean active = projectList.get(i).equals(projectName);
            if (hover) gui.fill(listX + 1, iy, listX + listW - 1, iy + itemH, 0xFF3A3A5A);
            if (active) gui.fill(listX + 1, iy, listX + 3, iy + itemH, 0xFF5599FF);
            gui.drawString(this.font, projectList.get(i), listX + 8, iy + 3, active ? 0xFF5599FF : 0xFFCCCCCC);
        }
    }

    private void renderHighlightedLine(GuiGraphics gui, String line, int x, int y) {
        String trimmed = line.trim();
        if (trimmed.startsWith("create")) {
            int createEnd = line.indexOf(' ') + 1;
            gui.drawString(this.font, line.substring(0, createEnd), x, y, 0xFF569CD6);
            int off = font.width(line.substring(0, createEnd));
            String rest = line.substring(createEnd);
            String tRest = rest.trim();
            if (tRest.startsWith("item") || tRest.startsWith("block") || tRest.startsWith("mob")) {
                int sp = rest.indexOf(tRest);
                if (sp > 0) { gui.drawString(this.font, rest.substring(0, sp), x + off, y, 0xFFD4D4D4); off += font.width(rest.substring(0, sp)); }
                int typeEnd = tRest.indexOf(' ') + 1;
                if (typeEnd <= 0) typeEnd = tRest.length();
                gui.drawString(this.font, tRest.substring(0, typeEnd), x + off, y, 0xFF4EC9B0);
                off += font.width(tRest.substring(0, typeEnd));
                rest = tRest.substring(typeEnd);
            }
            int q1 = rest.indexOf('"');
            int q2 = rest.lastIndexOf('"');
            if (q1 >= 0 && q2 > q1) {
                gui.drawString(this.font, rest.substring(0, q1 + 1), x + off, y, 0xFFD4D4D4);
                off += font.width(rest.substring(0, q1 + 1));
                gui.drawString(this.font, rest.substring(q1 + 1, q2), x + off, y, 0xFFCE9178);
                off += font.width(rest.substring(q1 + 1, q2));
                gui.drawString(this.font, rest.substring(q2), x + off, y, 0xFFD4D4D4);
            } else {
                gui.drawString(this.font, rest, x + off, y, 0xFFD4D4D4);
            }
        } else if (trimmed.startsWith("when")) {
            gui.drawString(this.font, line, x, y, 0xFFC586C0);
        } else if (trimmed.contains(":")) {
            int cp = line.indexOf(':');
            gui.drawString(this.font, line.substring(0, cp + 1), x, y, 0xFF9CDCFE);
            gui.drawString(this.font, line.substring(cp + 1), x + font.width(line.substring(0, cp + 1)), y, 0xFFB5CEA8);
        } else {
            gui.drawString(this.font, line, x, y, 0xFFD4D4D4);
        }
    }

    private void validateScript() {
        errors.clear();
        try {
            ModScriptLexer lexer = new ModScriptLexer(String.join("\n", lines));
            lexer.tokenize();
        } catch (Exception e) { errors.add(e.getMessage()); }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = 8;

        // Projects button
        int projW = font.width("Projects") + 12;
        if (mx >= x && mx <= x + projW && my >= BTN_Y && my <= BTN_Y + BTN_H) {
            showProjectList = !showProjectList;
            if (showProjectList) refreshProjectList();
            return true;
        }
        x += projW + 6;

        // New button
        int newW = font.width("New") + 10;
        if (mx >= x && mx <= x + newW && my >= BTN_Y && my <= BTN_Y + BTN_H) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) mc.player.connection.sendCommand("modcreator create NewProject");
            showStatus("Created NewProject");
            refreshProjectList();
            return true;
        }
        x += newW + 6 + 1 + 8;

        // Save
        int saveW = font.width("Save") + 12;
        if (mx >= x && mx <= x + saveW && my >= BTN_Y && my <= BTN_Y + BTN_H) { saveScript(); return true; }
        x += saveW + 4;

        // Run
        int runW = font.width("Run") + 12;
        if (mx >= x && mx <= x + runW && my >= BTN_Y && my <= BTN_Y + BTN_H) { runScript(); return true; }
        x += runW + 12;

        // Undo
        int undoW = font.width("Undo") + 10;
        if (mx >= x && mx <= x + undoW && my >= BTN_Y && my <= BTN_Y + BTN_H) { undo(); return true; }
        x += undoW + 4;

        // Redo
        int redoW = font.width("Redo") + 10;
        if (mx >= x && mx <= x + redoW && my >= BTN_Y && my <= BTN_Y + BTN_H) { redo(); return true; }
        x += redoW + 12;

        // Close
        int closeW = font.width("Close") + 10;
        if (mx >= x && mx <= x + closeW && my >= BTN_Y && my <= BTN_Y + BTN_H) { onClose(); return true; }

        // Project list click
        if (showProjectList) {
            int listX = 8;
            int listY = HEADER_HEIGHT;
            int itemH = 14;
            int listW = 150;
            for (int i = 0; i < projectList.size(); i++) {
                int iy = listY + 2 + i * itemH;
                if (mx >= listX && mx <= listX + listW && my >= iy && my <= iy + itemH) {
                    switchProject(projectList.get(i));
                    showProjectList = false;
                    return true;
                }
            }
            showProjectList = false;
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private List<String> undoStack = new ArrayList<>();
    private List<String> redoStack = new ArrayList<>();

    private void saveUndo() {
        undoStack.add(String.join("\n", lines));
        if (undoStack.size() > 50) undoStack.remove(0);
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.add(String.join("\n", lines));
        String prev = undoStack.remove(undoStack.size() - 1);
        lines.clear();
        for (String l : prev.split("\n")) lines.add(l);
        cursorLine = Math.min(cursorLine, lines.size() - 1);
        validateScript();
        showStatus("Undone");
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.add(String.join("\n", lines));
        String next = redoStack.remove(redoStack.size() - 1);
        lines.clear();
        for (String l : next.split("\n")) lines.add(l);
        cursorLine = Math.min(cursorLine, lines.size() - 1);
        validateScript();
        showStatus("Redone");
    }

    private void switchProject(String newProject) {
        saveCurrentScript();
        projectName = newProject;
        loadProjectScript();
        showStatus("Switched to: " + projectName);
    }

    private void saveCurrentScript() {
        String script = String.join("\n", lines);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand("modcreator save " + projectName + " " + escapeForCommand(script));
        }
    }

    private void saveScript() {
        saveCurrentScript();
        showStatus("Saved: " + projectName);
    }

    private void runScript() {
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 344) { runScript(); return true; }
        if (keyCode == 83 && hasControlDown()) { saveScript(); return true; }
        if (keyCode == 90 && hasControlDown()) { undo(); return true; }
        if (keyCode == 89 && hasControlDown()) { redo(); return true; }

        if (keyCode == 265) { if (cursorLine > 0) cursorLine--; cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length()); ensureCursorVisible(); validateScript(); return true; }
        if (keyCode == 264) { if (cursorLine < lines.size() - 1) cursorLine++; cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length()); ensureCursorVisible(); validateScript(); return true; }
        if (keyCode == 263) { if (cursorColumn > 0) cursorColumn--; else if (cursorLine > 0) { cursorLine--; cursorColumn = lines.get(cursorLine).length(); } return true; }
        if (keyCode == 262) { if (cursorColumn < lines.get(cursorLine).length()) cursorColumn++; else if (cursorLine < lines.size() - 1) { cursorLine++; cursorColumn = 0; } return true; }
        if (keyCode == 335 || keyCode == 331) { cursorColumn = 0; return true; }
        if (keyCode == 336 || keyCode == 332) { cursorColumn = lines.get(cursorLine).length(); return true; }

        if (keyCode == 257) {
            saveUndo();
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
            saveUndo();
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
            saveUndo();
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

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint >= 32 && codePoint < 127) {
            saveUndo();
            String line = lines.get(cursorLine);
            lines.set(cursorLine, line.substring(0, cursorColumn) + codePoint + line.substring(cursorColumn));
            cursorColumn++;
            validateScript();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
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
