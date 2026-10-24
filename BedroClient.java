package com.bedro;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class BedroClient implements ClientModInitializer {
    private boolean pWasDown;

    @Override
    public void onInitializeClient() {
        Modules.init();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        long window = GLFW.glfwGetCurrentContext();
        boolean down = window != 0 && GLFW.glfwGetKey(window, GLFW.GLFW_KEY_P) == GLFW.GLFW_PRESS;
        if (down && !pWasDown && mc.screen == null && mc.player != null) {
            mc.setScreen(new BedroScreen());
        }
        pWasDown = down;

        if (mc.player == null || mc.level == null) return;
        for (Module m : Modules.ALL) {
            if (m.enabled) m.onTick();
        }
    }
}
