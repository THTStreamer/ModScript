package com.modscript.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModCreatorScreen extends Screen {
    private final String projectName;

    public ModCreatorScreen(String projectName) {
        super(Component.literal("ModScript - " + projectName));
        this.projectName = projectName;
    }

    @Override
    protected void init() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        guiGraphics.drawString(this.font, "Project: " + projectName, 20, 40, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Press ESC to close", 20, 60, 0x888888);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
