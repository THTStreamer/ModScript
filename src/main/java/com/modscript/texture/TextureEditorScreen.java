package com.modscript.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class TextureEditorScreen extends Screen {
    private static final int CANVAS_SIZE = 16;
    private static final int PIXEL_SIZE = 16;
    private static final int PALETTE_SIZE = 10;
    private static final int TOOLBAR_HEIGHT = 28;
    private static final int SIDEBAR_WIDTH = 140;

    private final String textureName;
    private TextureData texture;
    private int currentColor = 0xFF000000;
    private int currentTool = 0;
    private boolean showGrid = true;
    private boolean dragging = false;
    private int lastMouseX, lastMouseY;
    private List<int[]> undoStack = new ArrayList<>();
    private List<int[]> redoStack = new ArrayList<>();
    private int selectedBase = -1;
    private boolean showBaseList = false;
    private List<String> baseNames = new ArrayList<>();
    private String statusMessage = "";
    private long statusTime = 0;
    private int canvasLeft, canvasTop;

    private static final String[] TOOLS = {"Pencil", "Eraser", "Fill", "Picker", "Line", "Rect", "Clear"};

    public TextureEditorScreen(String name) {
        super(Component.literal("Texture Editor - " + name));
        this.textureName = name;
        this.baseNames.addAll(TextureManager.getBaseTextures().keySet());
    }

    @Override
    protected void init() {
        if (texture == null) {
            texture = TextureManager.getTexture(textureName);
            if (texture == null) texture = TextureManager.createTexture(textureName);
        }
        canvasLeft = 8;
        canvasTop = TOOLBAR_HEIGHT + 8;
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        if (this.font == null) return;
        gui.fill(0, 0, this.width, this.height, 0xFF1A1A2E);
        renderToolbar(gui, mx, my);
        renderCanvas(gui, mx, my);
        renderSidebar(gui, mx, my);

        if (!statusMessage.isEmpty() && System.currentTimeMillis() - statusTime < 2000) {
            int w = font.width(statusMessage) + 16;
            int x = (this.width - w) / 2;
            gui.fill(x, this.height - 30, x + w, this.height - 14, 0xCC000000);
            gui.drawString(font, statusMessage, x + 8, this.height - 26, 0xFF55FF55);
        }
    }

    private void renderToolbar(GuiGraphics gui, int mx, int my) {
        gui.fill(0, 0, this.width, TOOLBAR_HEIGHT, 0xFF252535);
        gui.fill(0, TOOLBAR_HEIGHT - 1, this.width, TOOLBAR_HEIGHT, 0xFF444444);

        int x = 8;
        for (int i = 0; i < TOOLS.length; i++) {
            int w = font.width(TOOLS[i]) + 12;
            boolean hover = mx >= x && mx <= x + w && my >= 4 && my <= 22;
            boolean selected = i == currentTool;
            int bg = selected ? 0xFF4A6FA5 : (hover ? 0xFF3A3A4A : 0xFF2A2A3A);
            gui.fill(x, 4, x + w, 22, bg);
            gui.renderOutline(x, 4, w, 18, selected ? 0xFF6699CC : 0xFF555555);
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

    private void renderCanvas(GuiGraphics gui, int mx, int my) {
        int totalSize = CANVAS_SIZE * PIXEL_SIZE;

        gui.fill(canvasLeft - 2, canvasTop - 2, canvasLeft + totalSize + 2, canvasTop + totalSize + 2, 0xFF444444);
        gui.fill(canvasLeft - 1, canvasTop - 1, canvasLeft + totalSize + 1, canvasTop + totalSize + 1, 0xFF1A1A1A);

        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                int px = canvasLeft + x * PIXEL_SIZE;
                int py = canvasTop + y * PIXEL_SIZE;
                int color = texture.getPixel(x, y);

                if (color != 0) {
                    int a = (color >> 24) & 0xFF;
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    if (a == 0) a = 255;
                    gui.fill(px, py, px + PIXEL_SIZE - 1, py + PIXEL_SIZE - 1, (a << 24) | (r << 16) | (g << 8) | b);
                } else {
                    checkerPixel(gui, px, py);
                }

                boolean hover = mx >= px && mx < px + PIXEL_SIZE && my >= py && my < py + PIXEL_SIZE;
                if (hover) {
                    gui.fill(px, py, px + PIXEL_SIZE - 1, py + PIXEL_SIZE - 1, 0x44FFFFFF);
                }

                if (showGrid) {
                    gui.fill(px + PIXEL_SIZE - 1, py, px + PIXEL_SIZE, py + PIXEL_SIZE, 0xFF333333);
                    gui.fill(px, py + PIXEL_SIZE - 1, px + PIXEL_SIZE, py + PIXEL_SIZE, 0xFF333333);
                }
            }
        }
    }

    private void checkerPixel(GuiGraphics gui, int px, int py) {
        for (int cy = 0; cy < PIXEL_SIZE - 1; cy++) {
            for (int cx = 0; cx < PIXEL_SIZE - 1; cx++) {
                int c = ((cx / 4) + (cy / 4)) % 2 == 0 ? 0xFFCCCCCC : 0xFFAAAAAA;
                gui.fill(px + cx, py + cy, px + cx + 1, py + cy + 1, c);
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
        gui.fill(sx + 8, y, sx + 48, y + 20, currentColor);
        gui.renderOutline(sx + 8, y, 40, 20, 0xFFCCCCCC);
        y += 28;

        gui.drawString(font, "Palette:", sx + 8, y, 0xFFAAAAAA);
        y += 12;
        int palY = y;
        int col = 0;
        for (var entry : TextureManager.getPalette().entrySet()) {
            int pColor = 0xFF000000 | (entry.getValue()[0] << 16) | (entry.getValue()[1] << 8) | entry.getValue()[2];
            int px = sx + 8 + col * (PALETTE_SIZE + 2);
            if (px + PALETTE_SIZE > sx + SIDEBAR_WIDTH - 8) {
                col = 0;
                palY += PALETTE_SIZE + 2;
                px = sx + 8;
            }
            boolean hover = mx >= px && mx <= px + PALETTE_SIZE && my >= palY && my <= palY + PALETTE_SIZE;
            gui.fill(px, palY, px + PALETTE_SIZE, palY + PALETTE_SIZE, pColor);
            if (hover) {
                gui.renderOutline(px, palY, PALETTE_SIZE + 1, PALETTE_SIZE + 1, 0xFFFFFFFF);
            }
            col++;
        }
        y = palY + PALETTE_SIZE + 16;

        gui.drawString(font, "Base:", sx + 8, y, 0xFFAAAAAA);
        y += 12;
        gui.fill(sx + 8, y, sx + SIDEBAR_WIDTH - 8, y + 16, showBaseList ? 0xFF2A2A3A : 0xFF1A1A2A);
        gui.renderOutline(sx + 8, y, SIDEBAR_WIDTH - 16, 16, 0xFF555555);
        String baseLabel = selectedBase >= 0 && selectedBase < baseNames.size() ? baseNames.get(selectedBase) : "Click to select";
        gui.drawString(font, baseLabel.length() > 14 ? baseLabel.substring(0, 14) : baseLabel, sx + 12, y + 4, 0xFFCCCCCC);
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
        gui.drawString(font, "16x16 pixels", sx + 8, y, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

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
                    case 3 -> { int c = texture.getPixel(px, py); if (c != 0) currentColor = c; }
                    case 4, 5 -> dragging = true;
                }
                lastMouseX = px;
                lastMouseY = py;
                return true;
            }
        }

        if (my < TOOLBAR_HEIGHT) {
            handleToolbarClick(mx, my);
            return true;
        }

        if (mx >= this.width - SIDEBAR_WIDTH) {
            handleSidebarClick(mx, my);
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && button == 0) {
            int cx = 8, cy = TOOLBAR_HEIGHT + 8;
            int px = (int) ((mx - cx) / PIXEL_SIZE);
            int py = (int) ((my - cy) / PIXEL_SIZE);
            px = Math.max(0, Math.min(CANVAS_SIZE - 1, px));
            py = Math.max(0, Math.min(CANVAS_SIZE - 1, py));
            if (currentTool == 4) drawLine(lastMouseX, lastMouseY, px, py, currentColor);
            else if (currentTool == 5) drawRect(lastMouseX, lastMouseY, px, py, currentColor);
        }
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && (currentTool == 0 || currentTool == 1)) {
            int cx = 8, cy = TOOLBAR_HEIGHT + 8;
            int px = (int) ((mx - cx) / PIXEL_SIZE);
            int py = (int) ((my - cy) / PIXEL_SIZE);
            if (px >= 0 && px < CANVAS_SIZE && py >= 0 && py < CANVAS_SIZE) {
                texture.setPixel(px, py, currentTool == 0 ? currentColor : 0x00000000);
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private void handleToolbarClick(double mx, double my) {
        int x = 8;
        for (int i = 0; i < TOOLS.length; i++) {
            int w = font.width(TOOLS[i]) + 12;
            if (mx >= x && mx <= x + w) {
                if (i == 6) { saveUndo(); texture.clear(); showStatus("Canvas cleared"); }
                else currentTool = i;
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
        int y = TOOLBAR_HEIGHT + 8 + 12 + 28 + 12;
        int palY = y;
        int col = 0;
        for (var entry : TextureManager.getPalette().entrySet()) {
            int pColor = 0xFF000000 | (entry.getValue()[0] << 16) | (entry.getValue()[1] << 8) | entry.getValue()[2];
            int px = sx + 8 + col * (PALETTE_SIZE + 2);
            if (px + PALETTE_SIZE > sx + SIDEBAR_WIDTH - 8) {
                col = 0;
                palY += PALETTE_SIZE + 2;
                px = sx + 8;
            }
            if (mx >= px && mx <= px + PALETTE_SIZE && my >= palY && my <= palY + PALETTE_SIZE) {
                currentColor = pColor;
                return;
            }
            col++;
        }

        palY += PALETTE_SIZE + 16 + 12 + 20;
        if (mx >= sx + 8 && mx <= sx + SIDEBAR_WIDTH - 8 && my >= palY - 16 && my <= palY) {
            showBaseList = !showBaseList;
            return;
        }

        if (showBaseList) {
            int listY = palY + 4;
            for (int i = 0; i < Math.min(baseNames.size(), 8); i++) {
                int itemY = listY + i * 12;
                if (mx >= sx + 8 && mx <= sx + SIDEBAR_WIDTH - 8 && my >= itemY && my <= itemY + 12) {
                    selectedBase = i;
                    showBaseList = false;
                    return;
                }
            }
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            if (x0 >= 0 && x0 < CANVAS_SIZE && y0 >= 0 && y0 < CANVAS_SIZE)
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
            if (x >= 0 && x < CANVAS_SIZE) {
                if (minY >= 0 && minY < CANVAS_SIZE) texture.setPixel(x, minY, color);
                if (maxY >= 0 && maxY < CANVAS_SIZE) texture.setPixel(x, maxY, color);
            }
        }
        for (int y = minY; y <= maxY; y++) {
            if (y >= 0 && y < CANVAS_SIZE) {
                if (minX >= 0 && minX < CANVAS_SIZE) texture.setPixel(minX, y, color);
                if (maxX >= 0 && maxX < CANVAS_SIZE) texture.setPixel(maxX, y, color);
            }
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
        showStatus("Saved: " + textureName);
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusTime = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() { return true; }

    @Override
    public void onClose() {
        if (texture.isModified()) TextureManager.saveTexture(texture);
        super.onClose();
    }
}
