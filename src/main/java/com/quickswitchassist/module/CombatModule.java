package com.quickswitchassist.module;

import net.minecraft.client.Minecraft;

/**
 * 一个战斗辅助模块。所有辅助都实现这个接口、在 {@link ModuleRegistry} 里注册一行即可，
 * 入口类不用改。
 */
public interface CombatModule {

    /** 配置键前缀，模块之间不许重复。 */
    String id();

    /** 显示名。 */
    String displayName();

    boolean isEnabled();

    void setEnabled(boolean value);

    default void onClientTick(Minecraft client) {
    }

    /** 每渲染帧一次，deltaSeconds 是帧时长（秒）。 */
    default void onRenderFrame(Minecraft client, double deltaSeconds) {
    }

    /** HUD 行，调试用。 */
    default String[] hudLines() {
        return new String[0];
    }
}
