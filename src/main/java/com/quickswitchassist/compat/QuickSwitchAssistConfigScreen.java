package com.quickswitchassist.compat;

import com.quickswitchassist.config.CombatConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 参数都在这里轮换档位。
 *
 * <p>26.2：Screen 的渲染入口是 {@code extractRenderState(GuiGraphicsExtractor, ...)}，
 * 关屏回调是 {@code onClose()}，切屏走 {@code minecraft.gui.setScreen(...)}，
 * 字体字段是 {@code font}。背景由框架自己画（含模糊），这里不要再手动画背景 ——
 * 会在同一帧模糊第二次，直接崩 Can only blur once per frame。
 */
public class QuickSwitchAssistConfigScreen extends Screen {

    private static final int COL_W = 170;
    private static final int ROW_H = 20;
    private static final int GAP = 24;

    private final Screen parent;

    public QuickSwitchAssistConfigScreen(Screen parent) {
        super(Component.literal("quickswitch-assist 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - COL_W / 2;
        int top = this.height / 2 - 60;
        int row = 0;

        addBool(x, top + row++ * GAP, "总开关",
                v -> v ? "开" : "关",
                () -> CombatConfig.enabled, v -> CombatConfig.enabled = v);

        addBool(x, top + row++ * GAP, "秒切",
                v -> v ? "开" : "关",
                () -> CombatConfig.swapEnabled, v -> CombatConfig.swapEnabled = v);

        addRenderableWidget(Button.builder(Component.literal("秒切优先级表 →"), button -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(new PriorityScreen(this));
            }
        }).bounds(x, top + row++ * GAP, COL_W, ROW_H).build());

        addBool(x, top + row++ * GAP, "调试 HUD",
                v -> v ? "开" : "关",
                () -> CombatConfig.debugHud, v -> CombatConfig.debugHud = v);

        addBool(x, top + row++ * GAP, "诊断日志",
                v -> v ? "开" : "关",
                () -> CombatConfig.debugLog, v -> CombatConfig.debugLog = v);

        addRenderableWidget(Button.builder(Component.literal("完成"), button -> this.onClose())
                .bounds(x, top + row * GAP, COL_W, ROW_H).build());
    }

    private void addBool(int x, int y, String name, BoolLabel label,
                         BoolGetter getter, BoolSetter setter) {
        addRenderableWidget(Button.builder(Component.literal(text(name, label.of(getter.get()))), button -> {
            setter.set(!getter.get());
            CombatConfig.save();
            button.setMessage(Component.literal(text(name, label.of(getter.get()))));
        }).bounds(x, y, COL_W, ROW_H).build());
    }

    private static String text(String name, String value) {
        return name + ": " + value;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        CombatConfig.save();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @FunctionalInterface
    public interface BoolLabel {
        String of(boolean value);
    }

    @FunctionalInterface
    public interface BoolGetter {
        boolean get();
    }

    @FunctionalInterface
    public interface BoolSetter {
        void set(boolean value);
    }
}
