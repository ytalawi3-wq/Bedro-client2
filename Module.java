package com.bedro;

import net.minecraft.client.Minecraft;

public abstract class Module {
    public final String name;
    public boolean enabled;

    protected final Minecraft mc = Minecraft.getInstance();

    protected Module(String name) {
        this.name = name;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
}
