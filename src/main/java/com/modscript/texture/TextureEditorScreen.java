package com.modscript.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class TextureEditorScreen extends Screen {
    private static final int CANVAS_SIZE = 16;
    private static final int PIXEL_SIZE = 14;
    private static final int PALETTE_SIZE = 10;
    private static final int TOOLBAR_HEIGHT = 28;
    private static final int SIDEBAR_WIDTH = 140;

    private final String textureName;
    private TextureData texture;
    private int currentColor = 0xFF000000;
    private int currentTool = 0;
    private boolean showGrid = true;
    private int zoom = 1;
    private int panX = 0, panY = 0;
    private boolean dragging = false;
    private int lastMouseX, lastMouseY;
    private List<int[]> undoStack = new ArrayList<>();
    private List<int[]> redoStack = new ArrayList<>();
    private int paletteScroll = 0;
    private int selectedBase = -1;
    private List<String> baseNames = new ArrayList<>();
    private boolean showBaseList = false;
    private String statusMessage = "";
    private long statusTime = 0;

    private static final String[] TOOLS = {"Pencil", "Eraser", "Fill", "Picker", "Line", "Rect", "Clear"};
    private static final int[] TOOL_COLORS = {0xFF333333, 0xFF666666, 0xFF2E7D32, 0xFF1565C0, 0xFF6A1B9A, 0xFFBF360C, 0xFF880000};

    public TextureEditorScreen(String name) {
        super(Component.literal("Texture Editor - " + name));
        this.textureName = name;
        this.baseNames.addAll(TextureManager.getBaseTextures().keySet());
    }

    @Override
    protected void init() {
        if (texture == null) {
            texture = TextureManager.getTexture(textureName);
            if (texture == null) {
                texture = TextureManager.createTexture(textureName);
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui, mx, my, pt);
        int canvasX = 8;
        int canvasY = TOOLBAR_HEIGHT + 8;

        renderToolbar(gui, mx, my);
        renderCanvas(gui, canvasX, canvasY, mx, my);
        renderSidebar(gui, mx, my);

        if (!statusMessage.isEmpty() && System.currentTimeMillis() - statusTime < 2000) {
            int w = font.width(statusMessage) + 16;
            int x = (this.width - w) / 2;
            gui.fill(x, this.height - 30, x + w, this.height - 14, 0xCC000000);
            gui.drawString(font, statusMessage, x + 8, this.height - 26, 0xFF55FF55);
        }
    }

    private void renderToolbar(GuiGraphics gui, int mx, int my) {
        gui.fill(0, 0, this.width, TOOLBAR_HEIGHT, 0xFF1A1A2E);
        gui.fill(0, TOOLBAR_HEIGHT - 1, this.width, TOOLBAR_HEIGHT, 0xFF444444);

        int x = 8;
        for (int i = 0; i < TOOLS.length; i++) {
            int w = font.width(TOOLS[i]) + 12;
            boolean hover = mx >= x && mx <= x + w && my >= 4 && my <= 22;
            boolean selected = i == currentTool;
            int bg = selected ? 0xFF4A6FA5 : (hover ? 0xFF3A3A4A : 0xFF2A2A3A);
            int border = selected ? 0xFF6699CC : 0xFF555555;
            gui.fill(x, 4, x + w, 22, bg);
            gui.renderOutline(x, 4, w, 18, border);
            gui.drawString(font, TOOLS[i], x + 6, 9, 0xFFDDDDDD);
            x += w + 4;
        }

        x += 10;
        gui.fill(x, 4, x + 1, 22, 0xFF555555);
        x += 8;

        String gridText = showGrid ? "Grid: ON" : "Grid: OFF";
        int gw = font.width(gridText) + 12;
        boolean gHover = mx >= x && mx <= x + gw && my >= 4 && my <= 22;
        gui.fill(x, 4, x + gw, 22, gHover ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.renderOutline(x, 4, gw, 18, 0xFF555555);
        gui.drawString(font, gridText, x + 6, 9, showGrid ? 0xFF55FF55 : 0xFFFF5555);
        x += gw + 8;

        String undoText = "Undo(" + undoStack.size() + ")";
        int uw = font.width(undoText) + 12;
        boolean uHover = mx >= x && mx <= x + uw && my >= 4 && my <= 22;
        gui.fill(x, 4, x + uw, 22, uHover ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.renderOutline(x, 4, uw, 18, 0xFF555555);
        gui.drawString(font, undoText, x + 6, 9, 0xFFDDDDDD);
        x += uw + 4;

        String redoText = "Redo(" + redoStack.size() + ")";
        int rw = font.width(redoText) + 12;
        boolean rHover = mx >= x && mx <= x + rw && my >= 4 && my <= 22;
        gui.fill(x, 4, x + rw, 22, rHover ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.renderOutline(x, 4, rw, 18, 0xFF555555);
        gui.drawString(font, redoText, x + 6, 9, 0xFFDDDDDD);
        x += rw + 16;

        int saveW = font.width("Save") + 12;
        boolean sHover = mx >= x && mx <= x + saveW && my >= 4 && my <= 22;
        gui.fill(x, 4, x + saveW, 22, sHover ? 0xFF2E6B2E : 0xFF1E4E1E);
        gui.renderOutline(x, 4, saveW, 18, 0xFF338833);
        gui.drawString(font, "Save", x + 6, 9, 0xFF66DD66);
        x += saveW + 4;

        int closeW = font.width("Close") + 12;
        boolean cHover = mx >= x && mx <= x + closeW && my >= 4 && my <= 22;
        gui.fill(x, 4, x + closeW, 22, cHover ? 0xFF6B2E2E : 0xFF4E1E1E);
        gui.renderOutline(x, 4, closeW, 18, 0xFF883333);
        gui.drawString(font, "Close", x + 6, 9, 0xFFDD6666);
    }

    private void renderCanvas(GuiGraphics gui, int cx, int cy, int mx, int my) {
        int totalSize = CANVAS_SIZE * PIXEL_SIZE;
        gui.fill(cx - 1, cy - 1, cx + totalSize + 1, cy + totalSize + 1, 0xFF333333);
        gui.fill(cx, cy, cx + totalSize, cy + totalSize, 0xFF1A1A1A);

        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                int px = cx + x * PIXEL_SIZE;
                int py = cy + y * PIXEL_SIZE;
                int color = texture.getPixel(x, y);

                if (color != 0) {
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int a = (color >> 24) & 0xFF;
                    if (a == 0) a = 255;
                    int display = (a << 24) | (r << 16) | (g << 8) | b;
                    gui.fill(px, py, px + PIXEL_SIZE - 1, py + PIXEL_SIZE - 1, display);
                }

                boolean hover = mx >= px && mx < px + PIXEL_SIZE && my >= py && my < py + PIXEL_SIZE;
                if (hover) {
                    gui.fill(px, py, px + PIXEL_SIZE - 1, py + PIXEL_SIZE - 1, 0x44FFFFFF);
                }

                if (showGrid) {
                    gui.fill(px + PIXEL_SIZE - 1, py, px + PIXEL_SIZE, py + PIXEL_SIZE, 0xFF2A2A2A);
                    gui.fill(px, py + PIXEL_SIZE - 1, px + PIXEL_SIZE, py + PIXEL_SIZE, 0xFF2A2A2A);
                }
            }
        }
    }

    private void renderSidebar(GuiGraphics gui, int mx, int my) {
        int sx = this.width - SIDEBAR_WIDTH;
        gui.fill(sx, TOOLBAR_HEIGHT, this.width, this.height, 0xFF1E1E2E);
        gui.fill(sx, TOOLBAR_HEIGHT, sx + 1, this.height, 0xFF444444);

        int y = TOOLBAR_HEIGHT + 8;
        gui.drawString(font, "Color:", sx + 8, y, 0xFFAAAAAA);
        y += 12;
        gui.fill(sx + 8, y, sx + 8 + 40, y + 20, currentColor);
        gui.renderOutline(sx + 8, y, 40, 20, 0xFFCCCCCC);
        y += 28;

        gui.drawString(font, "Palette:", sx + 8, y, 0xFFAAAAAA);
        y += 12;
        int palY = y;
        int col = 0;
        for (var entry : TextureManager.getPalette().entrySet()) {
            int pColor = 0xFF000000 | (entry.getValue()[0] << 16) | (entry.getValue()[1] << 8) | entry.getValue()[2];
            int px = sx + 8 + col * (PALETTE_SIZE + 2);
            boolean hover = mx >= px && mx <= px + PALETTE_SIZE && my >= palY && my <= palY + PALETTE_SIZE;
            gui.fill(px, palY, px + PALETTE_SIZE, palY + PALETTE_SIZE, pColor);
            if (hover) {
                gui.renderOutline(px, palY, PALETTE_SIZE + 1, PALETTE_SIZE + 1, 0xFFFFFFFF);
                currentColor = pColor;
            }
            col++;
            if (col >= 10) { col = 0; palY += PALETTE_SIZE + 2; }
        }
        y = palY + PALETTE_SIZE + 12;

        gui.drawString(font, "Base Texture:", sx + 8, y, 0xFFAAAAAA);
        y += 12;
        gui.fill(sx + 8, y, sx + SIDEBAR_WIDTH - 8, y + 16, showBaseList ? 0xFF2A2A3A : 0xFF1A1A2A);
        gui.renderOutline(sx + 8, y, SIDEBAR_WIDTH - 16, 16, 0xFF555555);
        String baseLabel = selectedBase >= 0 ? baseNames.get(selectedBase) : "None";
        gui.drawString(font, baseLabel, sx + 12, y + 4, 0xFFCCCCCC);
        y += 20;

        if (showBaseList) {
            int listH = Math.min(baseNames.size(), 8) * 12 + 4;
            gui.fill(sx + 8, y, sx + SIDEBAR_WIDTH - 8, y + listH, 0xFF1A1A2A);
            gui.renderOutline(sx + 8, y, SIDEBAR_WIDTH - 16, listH, 0xFF555555);
            for (int i = 0; i < Math.min(baseNames.size(), 8); i++) {
                int itemY = y + 2 + i * 12;
                boolean hov = mx >= sx + 8 && mx <= sx + SIDEBAR_WIDTH - 8 && my >= itemY && my <= itemY + 12;
                if (hov) gui.fill(sx + 8, itemY, sx + SIDEBAR_WIDTH - 8, itemY + 12, 0xFF3A3A5A);
                gui.drawString(font, baseNames.get(i), sx + 12, itemY + 2, 0xFFCCCCCC);
            }
            y += listH + 4;
        }

        y += 8;
        gui.drawString(font, "Info:", sx + 8, y, 0xFFAAAAAA);
        y += 12;
        gui.drawString(font, "16x16 pixels", sx + 8, y, 0xFF888888);
        y += 12;
        gui.drawString(font, "Tools: " + TOOLS[currentTool], sx + 8, y, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int cx = 8;
        int cy = TOOLBAR_HEIGHT + 8;
        int totalSize = CANVAS_SIZE * PIXEL_SIZE;

        if (mx >= cx && mx <= cx + totalSize && my >= cy && my <= cy + totalSize) {
            int px = (int) ((mx - cx) / PIXEL_SIZE);
            int py = (int) ((my - cy) / PIXEL_SIZE);
            if (px >= 0 && px < CANVAS_SIZE && py >= 0 && py < CANVAS_SIZE) {
                saveUndo();
                switch (currentTool) {
                    case 0 -> texture.setPixel(px, py, currentColor);
                    case 1 -> texture.setPixel(px, py, 0x00000000);
                    case 2 -> texture.floodFill(px, py, currentColor);
                    case 3 -> currentColor = texture.getPixel(px, py);
                    case 4, 5 -> dragging = true;
                }
                lastMouseX = px;
                lastMouseY = py;
                return true;
            }
        }

        handleToolbarClick(mx, my);
        handleSidebarClick(mx, my);
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && currentTool == 4) {
            int cx = 8, cy = TOOLBAR_HEIGHT + 8;
            int px = (int) ((mx - cx) / PIXEL_SIZE);
            int py = (int) ((my - cy) / PIXEL_SIZE);
            drawLine(lastMouseX, lastMouseY, px, py, currentColor);
        } else if (dragging && currentTool == 5) {
            int cx = 8, cy = TOOLBAR_HEIGHT + 8;
            int px = (int) ((mx - cx) / PIXEL_SIZE);
            int py = (int) ((my - cy) / PIXEL_SIZE);
            drawRect(lastMouseX, lastMouseY, px, py, currentColor);
        }
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (currentTool == 0 || currentTool == 1) {
            int cx = 8, cy = TOOLBAR_HEIGHT + 8;
            int px = (int) ((mx - cx) / PIXEL_SIZE);
            int py = (int) ((my - cy) / PIXEL_SIZE);
            if (px >= 0 && px < CANVAS_SIZE && py >= 0 && py < CANVAS_SIZE) {
                int color = currentTool == 0 ? currentColor : 0x00000000;
                texture.setPixel(px, py, color);
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private void handleToolbarClick(double mx, double my) {
        if (my < 0 || my > TOOLBAR_HEIGHT) return;
        int x = 8;
        for (int i = 0; i < TOOLS.length; i++) {
            int w = font.width(TOOLS[i]) + 12;
            if (mx >= x && mx <= x + w) {
                if (i == 6) {
                    saveUndo();
                    texture.clear();
                    showStatus("Canvas cleared");
                } else {
                    currentTool = i;
                }
                return;
            }
            x += w + 4;
        }
        x += 10 + 1 + 8;
        int gw = font.width("Grid: ON") + 12;
        if (mx >= x && mx <= x + gw) { showGrid = !showGrid; return; }
        x += gw + 8;
        int uw = font.width("Undo(" + undoStack.size() + ")") + 12;
        if (mx >= x && mx <= x + uw) { undo(); return; }
        x += uw + 4;
        int rw = font.width("Redo(" + redoStack.size() + ")") + 12;
        if (mx >= x && mx <= x + rw) { redo(); return; }
        x += rw + 16;
        int sw = font.width("Save") + 12;
        if (mx >= x && mx <= x + sw) { save(); return; }
        x += sw + 4;
        int cw = font.width("Close") + 12;
        if (mx >= x && mx <= x + cw) { onClose(); return; }
    }

    private void handleSidebarClick(double mx, double my) {
        int sx = this.width - SIDEBAR_WIDTH;
        if (mx < sx) return;
        int y = TOOLBAR_HEIGHT + 8 + 12 + 28 + 12;
        int palY = y;
        int col = 0;
        for (var entry : TextureManager.getPalette().entrySet()) {
            int pColor = 0xFF000000 | (entry.getValue()[0] << 16) | (entry.getValue()[1] << 8) | entry.getValue()[2];
            int px = sx + 8 + col * (PALETTE_SIZE + 2);
            if (mx >= px && mx <= px + PALETTE_SIZE && my >= palY && my <= palY + PALETTE_SIZE) {
                currentColor = pColor;
                return;
            }
            col++;
            if (col >= 10) { col = 0; palY += PALETTE_SIZE + 2; }
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            texture.setPixel(x0, y0, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private void drawRect(int x0, int y0, int x1, int y1, int color) {
        int minX = Math.min(x0, x1), maxX = Math.max(x0, x1);
        int minY = Math.min(y0, y1), maxY = Math.max(y0, y1);
        for (int x = minX; x <= maxX; x++) {
            texture.setPixel(x, minY, color);
            texture.setPixel(x, maxY, color);
        }
        for (int y = minY; y <= maxY; y++) {
            texture.setPixel(minX, y, color);
            texture.setPixel(maxX, y, color);
        }
    }

    private void saveUndo() {
        undoStack.add(texture.getPixels());
        if (undoStack.size() > 50) undoStack.remove(0);
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.add(texture.getPixels());
        int[] prev = undoStack.remove(undoStack.size() - 1);
        texture = new TextureData(textureName, prev);
        showStatus("Undone");
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.add(texture.getPixels());
        int[] next = redoStack.remove(redoStack.size() - 1);
        texture = new TextureData(textureName, next);
        showStatus("Redone");
    }

    private void save() {
        TextureManager.saveTexture(texture);
        showStatus("Texture saved: " + textureName);
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusTime = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() { return true; }

    @Override
    public void onClose() {
        if (texture.isModified()) {
            TextureManager.saveTexture(texture);
        }
        super.onClose();
    }
}
