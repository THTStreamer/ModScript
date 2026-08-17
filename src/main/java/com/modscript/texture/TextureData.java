package com.modscript.texture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class TextureData {
    public static final int SIZE = 16;
    private final int[] pixels;
    private final String name;
    private String baseTexture;
    private boolean modified;

    public TextureData(String name) {
        this.name = name;
        this.pixels = new int[SIZE * SIZE];
        this.modified = false;
        Arrays.fill(pixels, 0x00000000);
    }

    public TextureData(String name, int[] pixels) {
        this.name = name;
        this.pixels = Arrays.copyOf(pixels, SIZE * SIZE);
        this.modified = false;
    }

    public TextureData(String name, BufferedImage image) {
        this.name = name;
        this.pixels = new int[SIZE * SIZE];
        loadFromImage(image);
        this.modified = false;
    }

    public void loadFromImage(BufferedImage image) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int srcX = x * image.getWidth() / SIZE;
                int srcY = y * image.getHeight() / SIZE;
                if (srcX < image.getWidth() && srcY < image.getHeight()) {
                    pixels[y * SIZE + x] = image.getRGB(srcX, srcY);
                }
            }
        }
        modified = true;
    }

    public BufferedImage toImage() {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, pixels[y * SIZE + x]);
            }
        }
        return image;
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return 0;
        return pixels[y * SIZE + x];
    }

    public void setPixel(int x, int y, int color) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        int old = pixels[y * SIZE + x];
        if (old != color) {
            pixels[y * SIZE + x] = color;
            modified = true;
        }
    }

    public int getPixelARGB(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int getAlpha(int color) { return (color >> 24) & 0xFF; }
    public int getRed(int color) { return (color >> 16) & 0xFF; }
    public int getGreen(int color) { return (color >> 8) & 0xFF; }
    public int getBlue(int color) { return color & 0xFF; }

    public void floodFill(int startX, int startY, int newColor) {
        if (startX < 0 || startX >= SIZE || startY < 0 || startY >= SIZE) return;
        int targetColor = getPixel(startX, startY);
        if (targetColor == newColor) return;
        java.util.Stack<int[]> stack = new java.util.Stack<>();
        stack.push(new int[]{startX, startY});
        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int x = pos[0], y = pos[1];
            if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
            if (getPixel(x, y) != targetColor) continue;
            setPixel(x, y, newColor);
            stack.push(new int[]{x + 1, y});
            stack.push(new int[]{x - 1, y});
            stack.push(new int[]{x, y + 1});
            stack.push(new int[]{x, y - 1});
        }
    }

    public void clear() {
        Arrays.fill(pixels, 0x00000000);
        modified = true;
    }

    public void flipHorizontal() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE / 2; x++) {
                int tmp = pixels[y * SIZE + x];
                pixels[y * SIZE + x] = pixels[y * SIZE + (SIZE - 1 - x)];
                pixels[y * SIZE + (SIZE - 1 - x)] = tmp;
            }
        }
        modified = true;
    }

    public void flipVertical() {
        for (int y = 0; y < SIZE / 2; y++) {
            for (int x = 0; x < SIZE; x++) {
                int tmp = pixels[y * SIZE + x];
                pixels[y * SIZE + x] = pixels[(SIZE - 1 - y) * SIZE + x];
                pixels[(SIZE - 1 - y) * SIZE + x] = tmp;
            }
        }
        modified = true;
    }

    public void rotate90() {
        int[] rotated = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                rotated[x * SIZE + (SIZE - 1 - y)] = pixels[y * SIZE + x];
            }
        }
        System.arraycopy(rotated, 0, pixels, 0, SIZE * SIZE);
        modified = true;
    }

    public void copyFrom(TextureData other) {
        System.arraycopy(other.pixels, 0, this.pixels, 0, SIZE * SIZE);
        modified = true;
    }

    public TextureData copy() {
        return new TextureData(name, Arrays.copyOf(pixels, SIZE * SIZE));
    }

    public int[] getPixels() { return Arrays.copyOf(pixels, SIZE * SIZE); }
    public String getName() { return name; }
    public String getBaseTexture() { return baseTexture; }
    public void setBaseTexture(String base) { this.baseTexture = base; }
    public boolean isModified() { return modified; }
    public void setModified(boolean m) { modified = m; }
}
