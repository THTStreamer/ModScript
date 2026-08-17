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
    private boolean needsRender = true;
    private long lastRenderTime = 0;

    private static final String[] TOOLS = {"Pencil", "Eraser", "Fill", "Picker", "Line", "Rect", "Clear"};

    public TextureEditorScreen(String name) {
        super(Component.literal("Texture Editor"));
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

        long now = System.currentTimeMillis();
        if (!needsRender && now - lastRenderTime < 16) return;
        lastRenderTime = now;
        needsRender = false;

        gui.fill(0, 0, this.width, this.height, 0xFF1A1A2E);
        renderToolbar(gui, mx, my);
        renderCanvas(gui, mx, my);
        renderSidebar(gui, mx, my);

        if (!statusMessage.isEmpty() && now - statusTime < 2000) {
            int w = font.width(statusMessage) + 16;
            int x = (this.width - w) / 2;
            gui.fill(x, this.height - 30, x + w, this.height - 14, 0xCC000000);
            gui.drawString(font, statusMessage, x + 8, this.height - 26, 0xFF55FF55);
        }
    }

    private void renderToolbar(GuiGraphics gui, int mx, int my) {
        gui.fill(0, 0, this.width, TOOLBAR_HEIGHT, 0xFF252535);

        int x = 8;
        for (int i = 0; i < TOOLS.length; i++) {
            int w = font.width(TOOLS[i]) + 12;
            boolean hover = mx >= x && mx <= x + w && my >= 4 && my <= 22;
            boolean selected = i == currentTool;
            int bg = selected ? 0xFF4A6FA5 : (hover ? 0xFF3A3A4A : 0xFF2A2A3A);
            gui.fill(x, 4, x + w, 22, bg);
            gui.drawString(font, TOOLS[i], x + 6, 9, 0xFFDDDDDD);
            x += w + 4;
        }

        x += 8;
        String gridText = showGrid ? "Grid ON" : "Grid OFF";
        int gw = font.width(gridText) + 12;
        gui.fill(x, 4, x + gw, 22, mx >= x && mx <= x + gw && my >= 4 && my <= 22 ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.drawString(font, gridText, x + 6, 9, showGrid ? 0xFF55FF55 : 0xFFFF5555);
        x += gw + 8;

        int uw = font.width("Undo") + 10;
        gui.fill(x, 4, x + uw, 22, mx >= x && mx <= x + uw && my >= 4 && my <= 22 ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.drawString(font, "Undo", x + 5, 9, undoStack.isEmpty() ? 0xFF666666 : 0xFFDDDDDD);
        x += uw + 4;

        int rw = font.width("Redo") + 10;
        gui.fill(x, 4, x + rw, 22, mx >= x && mx <= x + rw && my >= 4 && my <= 22 ? 0xFF3A3A4A : 0xFF2A2A3A);
        gui.drawString(font, "Redo", x + 5, 9, redoStack.isEmpty() ? 0xFF666666 : 0xFFDDDDDD);
        x += rw + 12;

        gui.fill(x, 4, x + 40, 22, mx >= x && mx <= x + 40 && my >= 4 && my <= 22 ? 0xFF2E6B2E : 0xFF1E4E1E);
        gui.drawString(font, "Save", x + 12, 9, 0xFF66DD66);
        x += 44;

        gui.fill(x, 4, x + 44, 22, mx >= x && mx <= x + 44 && my >= 4 && my <= 22 ? 0xFF6B2E2E : 0xFF4E1E1E);
        gui.drawString(font, "Close", x + 8, 9, 0xFFDD6666);
    }

    private void renderCanvas(GuiGraphics gui, int mx, int my) {
        int totalSize = CANVAS_SIZE * PIXEL_SIZE;
        gui.fill(canvasLeft - 2, canvasTop - 2, canvasLeft + totalSize + 2, canvasTop + totalSize + 2, 0xFF444444);

        int hoverX = -1, hoverY = -1;
        if (mx >= canvasLeft && mx < canvasLeft + totalSize && my >= canvasTop && my < canvasTop + totalSize) {
            hoverX = (int) ((mx - canvasLeft) / PIXEL_SIZE);
            hoverY = (int) ((my - canvasTop) / PIXEL_SIZE);
        }

        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                int px = canvasLeft + x * PIXEL_SIZE;
                int py = canvasTop + y * PIXEL_SIZE;
                int color = texture.getPixel(x, y);

                if (color != 0) {
                    int a = (color >> 24) & 0xFF;
                    if (a == 0) a = 255;
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    gui.fill(px, py, px + PIXEL_SIZE - 1, py + PIXEL_SIZE - 1, (a << 24) | (r << 16) | (g << 8) | b);
                } else {
                    boolean checker = ((x / 2) + (y / 2)) % 2 == 0;
                    gui.fill(px, py, px + PIXEL_SIZE - 1, py + PIXEL_SIZE - 1, checker ? 0xFFCCCCCC : 0xFF999999);
                }

                if (x == hoverX && y == hoverY) {
                    gui.fill(px, py, px + PIXEL_SIZE - 1, py + PIXEL_SIZE - 1, 0x44FFFFFF);
                }

                if (showGrid) {
                    gui.fill(px + PIXEL_SIZE - 1, py, px + PIXEL_SIZE, py + PIXEL_SIZE, 0xFF333333);
                    gui.fill(px, py + PIXEL_SIZE - 1, px + PIXEL_SIZE, py + PIXEL_SIZE, 0xFF333333);
                }
            }
        }
    }

    private void renderSidebar(GuiGraphics gui, int mx, int my) {
        int sx = this.width - SIDEBAR_WIDTH;
        gui.fill(sx, TOOLBAR_HEIGHT, this.width, this.height, 0xFF1E1E2E);

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
            gui.fill(px, palY, px + PALETTE_SIZE, palY + PALETTE_SIZE, pColor);
            if (mx >= px && mx <= px + PALETTE_SIZE && my >= palY && my <= palY + PALETTE_SIZE) {
                gui.renderOutline(px, palY, PALETTE_SIZE + 1, PALETTE_SIZE + 1, 0xFFFFFFFF);
            }
            col++;
        }
        y = palY + PALETTE_SIZE + 16;

        gui.drawString(font, "Base:", sx + 8, y, 0xFFAAAAAA);
        y += 12;
        gui.fill(sx + 8, y, sx + SIDEBAR_WIDTH - 8, y + 16, 0xFF1A1A2A);
        gui.renderOutline(sx + 8, y, SIDEBAR_WIDTH - 16, 16, 0xFF555555);
        String baseLabel = selectedBase >= 0 && selectedBase < baseNames.size() ? baseNames.get(selectedBase) : "Click to pick";
        gui.drawString(font, baseLabel.length() > 14 ? baseLabel.substring(0, 14) : baseLabel, sx + 12, y + 4, 0xFFCCCCCC);
        y += 20;

        if (showBaseList) {
            int listH = Math.min(baseNames.size(), 8) * 12 + 4;
            gui.fill(sx + 8, y, sx + SIDEBAR_WIDTH - 8, y + listH, 0xFF1A1A2A);
            for (int i = 0; i < Math.min(baseNames.size(), 8); i++) {
                int itemY = y + 2 + i * 12;
                if (mx >= sx + 8 && mx <= sx + SIDEBAR_WIDTH - 8 && my >= itemY && my <= itemY + 12) {
                    gui.fill(sx + 8, itemY, sx + SIDEBAR_WIDTH - 8, itemY + 12, 0xFF3A3A5A);
                }
                gui.drawString(font, baseNames.get(i), sx + 12, itemY + 2, 0xFFCCCCCC);
            }
            y += listH + 4;
        }

        y += 8;
        gui.drawString(font, "16x16", sx + 8, y, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int totalSize = CANVAS_SIZE * PIXEL_SIZE;
        if (mx >= canvasLeft && mx <= canvasLeft + totalSize && my >= canvasTop && my <= canvasTop + totalSize) {
            int px = (int) ((mx - canvasLeft) / PIXEL_SIZE);
            int py = (int) ((my - canvasTop) / PIXEL_SIZE);
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
                needsRender = true;
                return true;
            }
        }

        if (my < TOOLBAR_HEIGHT) { handleToolbarClick(mx, my); return true; }
        if (mx >= this.width - SIDEBAR_WIDTH) { handleSidebarClick(mx, my); return true; }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && button == 0) {
            int px = Math.max(0, Math.min(CANVAS_SIZE - 1, (int) ((mx - canvasLeft) / PIXEL_SIZE)));
            int py = Math.max(0, Math.min(CANVAS_SIZE - 1, (int) ((my - canvasTop) / PIXEL_SIZE)));
            if (currentTool == 4) drawLine(lastMouseX, lastMouseY, px, py, currentColor);
            else if (currentTool == 5) drawRect(lastMouseX, lastMouseY, px, py, currentColor);
            needsRender = true;
        }
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && (currentTool == 0 || currentTool == 1)) {
            int px = (int) ((mx - canvasLeft) / PIXEL_SIZE);
            int py = (int) ((my - canvasTop) / PIXEL_SIZE);
            if (px >= 0 && px < CANVAS_SIZE && py >= 0 && py < CANVAS_SIZE) {
                texture.setPixel(px, py, currentTool == 0 ? currentColor : 0x00000000);
                needsRender = true;
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        int totalSize = CANVAS_SIZE * PIXEL_SIZE;
        if (mx >= canvasLeft && mx < canvasLeft + totalSize && my >= canvasTop && my < canvasTop + totalSize) {
            needsRender = true;
        }
        super.mouseMoved(mx, my);
    }

    private void handleToolbarClick(double mx, double my) {
        int x = 8;
        for (int i = 0; i < TOOLS.length; i++) {
            int w = font.width(TOOLS[i]) + 12;
            if (mx >= x && mx <= x + w) {
                if (i == 6) { saveUndo(); texture.clear(); showStatus("Cleared"); }
                else currentTool = i;
                needsRender = true;
                return;
            }
            x += w + 4;
        }
        x += 8;
        int gw = font.width("Grid ON") + 12;
        if (mx >= x && mx <= x + gw) { showGrid = !showGrid; needsRender = true; return; }
        x += gw + 8;
        int uw = font.width("Undo") + 10;
        if (mx >= x && mx <= x + uw) { undo(); return; }
        x += uw + 4;
        int rw = font.width("Redo") + 10;
        if (mx >= x && mx <= x + rw) { redo(); return; }
        x += rw + 12;
        if (mx >= x && mx <= x + 40) { save(); return; }
        x += 44;
        if (mx >= x && mx <= x + 44) { onClose(); return; }
    }

    private void handleSidebarClick(double mx, double my) {
        int sx = this.width - SIDEBAR_WIDTH;
        int y = TOOLBAR_HEIGHT + 8 + 12 + 28 + 12;
        int palY = y;
        int col = 0;
        for (var entry : TextureManager.getPalette().entrySet()) {
            int pColor = 0xFF000000 | (entry.getValue()[0] << 16) | (entry.getValue()[1] << 8) | entry.getValue()[2];
            int px = sx + 8 + col * (PALETTE_SIZE + 2);
            if (px + PALETTE_SIZE > sx + SIDEBAR_WIDTH - 8) { col = 0; palY += PALETTE_SIZE + 2; px = sx + 8; }
            if (mx >= px && mx <= px + PALETTE_SIZE && my >= palY && my <= palY + PALETTE_SIZE) {
                currentColor = pColor;
                needsRender = true;
                return;
            }
            col++;
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            if (x0 >= 0 && x0 < CANVAS_SIZE && y0 >= 0 && y0 < CANVAS_SIZE) texture.setPixel(x0, y0, color);
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
        texture = new TextureData(textureName, undoStack.remove(undoStack.size() - 1));
        needsRender = true;
        showStatus("Undone");
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.add(texture.getPixels());
        texture = new TextureData(textureName, redoStack.remove(redoStack.size() - 1));
        needsRender = true;
        showStatus("Redone");
    }

    private void save() {
        TextureManager.saveTexture(texture);
        showStatus("Saved: " + textureName);
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusTime = System.currentTimeMillis();
        needsRender = true;
    }

    @Override
    public boolean isPauseScreen() { return true; }

    @Override
    public void onClose() {
        if (texture.isModified()) TextureManager.saveTexture(texture);
        super.onClose();
    }
}
