package com.quickswitchassist.util;

import com.quickswitchassist.config.CombatConfig;
import com.quickswitchassist.module.CombatModule;
import com.quickswitchassist.module.ModuleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DebugHud {

    private static final int GREEN = 0xFF55FF55;
    private static final int GRAY = 0xFFAAAAAA;

    private DebugHud() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!CombatConfig.debugHud) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        // 26.2：options.hudHidden 没了，F1 的状态在 Hud 上；世界字段叫 level。
        if (client.player == null || client.level == null || client.gui.hud.isHidden()) {
            return;
        }

        int x = 6;
        int y = 6;

        graphics.text(client.font,
                "quickswitch-assist  " + (CombatConfig.enabled ? "ON" : "OFF"),
                x, y, CombatConfig.enabled ? GREEN : GRAY, true);
        y += 10;

        for (CombatModule module : ModuleRegistry.all()) {
            for (String line : module.hudLines()) {
                graphics.text(client.font, line, x, y,
                        module.isEnabled() ? GREEN : GRAY, true);
                y += 10;
            }
        }
    }
}
