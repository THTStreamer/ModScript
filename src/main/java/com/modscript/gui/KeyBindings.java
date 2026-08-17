package com.modscript.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyMapping openEditor;

    public static void register(RegisterKeyMappingsEvent event) {
        openEditor = new KeyMapping(
            "key.modscript.editor",
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.modscript"
        );
        event.register(openEditor);
    }

    @SubscribeEvent
    public static void onKeyPress(net.neoforged.neoforge.client.event.InputEvent.Key event) {
        if (openEditor != null && openEditor.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.setScreen(new CodeEditorScreen("Current Project"));
            }
        }
    }
}
