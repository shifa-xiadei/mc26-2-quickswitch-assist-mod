package com.quickswitchassist.compat;

import com.quickswitchassist.config.CombatConfig;
import com.quickswitchassist.module.rule.PriorityTable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 秒切优先级表：两种视图。
 *
 * <ul>
 *   <li><b>情境表</b>：一行一个情境，5 列是候选武器（空手/剑/斧/锤/矛）的分。
 *       点数字 = +10（到 100 回到 0）。</li>
 *   <li><b>手持权重表</b>：一行一个"当前手持"，5 列是"允不允许切过去"，
 *       点数字 = +1（到 3 回到 0）；<b>0 = 拿这把时永远不切过去</b>。</li>
 * </ul>
 *
 * <p>26.2：渲染入口改名 extractRenderState，重建控件用 rebuildWidgets()，字体字段 font。
 */
public class PriorityScreen extends Screen {

    private static final int ROW_H = 20;
    private static final int GAP = 22;
    private static final int LABEL_W = 78;
    private static final int W_NUM = 34;
    private static final int SPACING = 3;
    private static final int TOP = 52;

    private final Screen parent;
    private boolean weightMode;

    public PriorityScreen(Screen parent) {
        super(Component.literal("秒切优先级表"));
        this.parent = parent;
    }

    private static int totalWidth() {
        return LABEL_W + SPACING + PriorityTable.Kind.values().length * (W_NUM + SPACING);
    }

    @Override
    protected void init() {
        int x0 = this.width / 2 - totalWidth() / 2;
        int numX = x0 + LABEL_W + SPACING;

        if (weightMode) {
            PriorityTable.Kind[] rows = PriorityTable.Kind.values();
            for (int r = 0; r < rows.length; r++) {
                int[] weights = PriorityTable.weights(rows[r]);
                for (int c = 0; c < weights.length; c++) {
                    addNum(numX + c * (W_NUM + SPACING), TOP + r * GAP, weights, c, 3);
                }
            }
        } else {
            PriorityTable.Situation[] rows = PriorityTable.Situation.values();
            for (int r = 0; r < rows.length; r++) {
                int[] scores = PriorityTable.scores(rows[r]);
                for (int c = 0; c < scores.length; c++) {
                    addNum(numX + c * (W_NUM + SPACING), TOP + r * GAP, scores, c, 100);
                }
            }
        }

        int rows = weightMode ? PriorityTable.Kind.values().length
                : PriorityTable.Situation.values().length;
        int bottom = TOP + rows * GAP + 10;

        addRenderableWidget(Button.builder(
                Component.literal(weightMode ? "看情境表" : "看手持权重表"), b -> {
                    weightMode = !weightMode;
                    this.rebuildWidgets();
                }).bounds(this.width / 2 - 118, bottom, 112, ROW_H).build());

        addRenderableWidget(Button.builder(Component.literal("恢复默认"), b -> {
            PriorityTable.resetToDefaults();
            save();
            this.rebuildWidgets();
        }).bounds(this.width / 2 - 2, bottom, 56, ROW_H).build());

        addRenderableWidget(Button.builder(Component.literal("完成"), b -> this.onClose())
                .bounds(this.width / 2 + 58, bottom, 56, ROW_H).build());
    }

    /** 点一下加一档；到顶回到 0。 */
    private void addNum(int x, int y, int[] values, int index, int max) {
        addRenderableWidget(Button.builder(Component.literal(String.valueOf(values[index])), b -> {
            values[index] = values[index] >= max ? 0 : values[index] + (max == 3 ? 1 : 10);
            save();
            b.setMessage(Component.literal(String.valueOf(values[index])));
        }).bounds(x, y, W_NUM, ROW_H).build());
    }

    private static void save() {
        CombatConfig.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font,
                Component.literal(weightMode ? "手持权重表（0 = 永不切过去）" : "情境优先级表（越大越优先，0 = 不切）"),
                this.width / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.literal(weightMode ? "行 = 当前手持，列 = 允许切过去的目标" : "从上到下第一条成立的生效"),
                this.width / 2, 34, 0xFFAAAAAA);

        int x0 = this.width / 2 - totalWidth() / 2;
        int numX = x0 + LABEL_W + SPACING;

        for (int c = 0; c < PriorityTable.Kind.values().length; c++) {
            graphics.text(this.font, PriorityTable.Kind.values()[c].label,
                    numX + c * (W_NUM + SPACING) + 6, TOP - 12, 0xFFAAAAAA, true);
        }
        if (weightMode) {
            PriorityTable.Kind[] rows = PriorityTable.Kind.values();
            for (int r = 0; r < rows.length; r++) {
                graphics.text(this.font, rows[r].label,
                        x0, TOP + r * GAP + 6, 0xFFFFFFFF, true);
            }
        } else {
            PriorityTable.Situation[] rows = PriorityTable.Situation.values();
            for (int r = 0; r < rows.length; r++) {
                graphics.text(this.font, rows[r].label,
                        x0, TOP + r * GAP + 6, 0xFFFFFFFF, true);
            }
        }
    }

    @Override
    public void onClose() {
        save();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }
}
