package com.bedro;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BedroScreen extends Screen {
    private static final int COLS = 3, W = 110, H = 20, GAP = 4;

    public BedroScreen() {
        super(Component.literal("Bedro Client"));
    }

    @Override
    protected void init() {
        int total = COLS * W + (COLS - 1) * GAP;
        int x0 = (this.width - total) / 2;
        int y0 = 40;
        addRenderableWidget(Button.builder(Component.literal("== Bedro Client (ESC to close) =="), b -> {})
                .bounds(x0, 12, total, H).build()).active = false;
        int i = 0;
        for (Module m : Modules.ALL) {
            int x = x0 + (i % COLS) * (W + GAP);
            int y = y0 + (i / COLS) * (H + GAP);
            addRenderableWidget(Button.builder(label(m), b -> {
                m.toggle();
                b.setMessage(label(m));
            }).bounds(x, y, W, H).build());
            i++;
        }
    }

    private static Component label(Module m) {
        return Component.literal((m.enabled ? "§a" : "§c") + m.name);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
