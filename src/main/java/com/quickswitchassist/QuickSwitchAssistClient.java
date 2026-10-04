package com.quickswitchassist;

import com.quickswitchassist.config.CombatConfig;
import com.quickswitchassist.util.DebugHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickSwitchAssistClient implements ClientModInitializer {

    public static final String MOD_ID = "quickswitch-assist";
    public static final Logger LOGGER = LoggerFactory.getLogger("quickswitch-assist");

    /** 供 Mixin 取客户端实例用。 */
    public static Minecraft client() {
        return Minecraft.getInstance();
    }

    public static void debug(String format, Object... args) {
        if (Edition.DEV && CombatConfig.debugLog) {
            LOGGER.info(format, args);
        }
    }

    @Override
    public void onInitializeClient() {
        CombatConfig.load();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            for (var module : com.quickswitchassist.module.ModuleRegistry.all()) {
                module.onClientTick(client);
            }
        });

        // 26.2 的 HUD API 改为 attachElementBefore/After，
        // 元素签名也从 (DrawContext, RenderTickCounter) 变成 (GuiGraphicsExtractor, DeltaTracker)。
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "debug_hud"),
                (graphics, tickCounter) -> DebugHud.render(graphics));

        LOGGER.info("[quickswitch-assist] {} loaded: CLIENT-ONLY.", Edition.TITLE);
    }
}
