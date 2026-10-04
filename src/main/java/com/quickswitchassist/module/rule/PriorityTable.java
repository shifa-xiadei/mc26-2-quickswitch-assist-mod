package com.quickswitchassist.module.rule;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 秒切优先级表。
 *
 * <p>「情境 → 5 把候选武器（空手/剑/斧/锤/矛）各一个分」，取分最高的那把切过去；
 * 分 &lt;= 0 表示不切。情境行<b>从上到下第一条成立的说了算</b>。
 *
 * <p>另有一张 5×5 的「手持 → 候选」权重表：0 = 拿着这把时永远不切过去，
 * 大于 1 = 加权（乘法）。
 */
public final class PriorityTable {

    /** 下落多少格才算「下落中」。 */
    public static final double FALL_DISTANCE = 1.5D;

    /** 超过多少格算「够不着，只能用长矛」。 */
    public static final double FAR_DISTANCE = 3.0D;

    /** 候选武器类型（表里的 5 列）。 */
    public enum Kind {
        EMPTY("空手"), SWORD("剑"), AXE("斧头"), MACE("重锤"), SPEAR("长矛");

        public final String label;

        Kind(String label) {
            this.label = label;
        }
    }

    /** 情境（表里的行，从上到下第一条成立的生效）。 */
    public enum Situation {
        TARGET_BLOCKING("目标举盾"),
        SHIELD_BROKEN_AIR("空中·盾刚破"),
        FALLING("下落中"),
        AIRBORNE("空中"),
        TARGET_FAR("目标>3格"),
        TARGET_ARMORED("目标有甲"),
        TARGET_UNARMORED("目标无甲"),
        FALLBACK("其他");

        public final String label;

        Situation(String label) {
            this.label = label;
        }
    }

    private static final int ROWS = Situation.values().length;
    private static final int COLS = Kind.values().length;

    private static final int[][] SCORES = new int[ROWS][COLS];
    private static final int[][] WEIGHTS = new int[COLS][COLS];

    static {
        resetToDefaults();
    }

    private PriorityTable() {
    }

    /** 默认表。数字按「这个情境下这把武器值多少」给，0 = 不切。 */
    public static void resetToDefaults() {
        row(Situation.TARGET_BLOCKING, 0, 40, 100, 20, 20);
        row(Situation.SHIELD_BROKEN_AIR, 0, 70, 40, 100, 30);
        row(Situation.FALLING, 0, 50, 60, 100, 30);
        row(Situation.AIRBORNE, 0, 50, 60, 90, 30);
        row(Situation.TARGET_FAR, 0, 20, 20, 20, 100);
        row(Situation.TARGET_ARMORED, 0, 60, 50, 70, 40);
        row(Situation.TARGET_UNARMORED, 0, 100, 60, 40, 40);
        // 「其他」= 准星里没有目标。给长矛留 30：空手/拿着非武器时的突进长矛就靠这一行，
        // 没有它「空手 + 突进矛」那条行为会整个失效（长矛的无突进/手持武器的门槛在 SwapAssist 里）。
        row(Situation.FALLBACK, 0, 0, 0, 0, 30);
        for (int from = 0; from < COLS; from++) {
            for (int to = 0; to < COLS; to++) {
                WEIGHTS[from][to] = from == to ? 0 : 1;
            }
        }
    }

    private static void row(Situation situation, int empty, int sword, int axe, int mace, int spear) {
        int[] target = SCORES[situation.ordinal()];
        target[Kind.EMPTY.ordinal()] = empty;
        target[Kind.SWORD.ordinal()] = sword;
        target[Kind.AXE.ordinal()] = axe;
        target[Kind.MACE.ordinal()] = mace;
        target[Kind.SPEAR.ordinal()] = spear;
    }

    /** 结果只打印用；直接改返回的数组即可生效。 */
    public static int[] scores(Situation situation) {
        return SCORES[situation.ordinal()];
    }

    public static int score(Situation situation, Kind kind) {
        return SCORES[situation.ordinal()][kind.ordinal()];
    }

    public static int[] weights(Kind held) {
        return WEIGHTS[held.ordinal()];
    }

    public static int weight(Kind held, Kind candidate) {
        return WEIGHTS[held.ordinal()][candidate.ordinal()];
    }

    /**
     * 当前情境：从上到下第一条成立的。
     *
     * <p>{@code aimed} = 准星（放宽过的）命中的实体，{@code inFront} = 前方一大片范围内最近的实体，
     * {@code airborne} = 离地（带几 tick 记忆，免得一落地就换行）。
     * <b>「下落中 / 空中」两行只用 {@code inFront}</b> —— 用准星判定太严，用户实测报过
     * "跳着打人却经常不切"；地面那几行仍然要求 {@code aimed}，免得对着空气也切。
     */
    public static Situation situation(Player player, Entity aimed, Entity inFront,
                                      int shieldBrokenTicks, boolean airborne) {
        if (aimed instanceof LivingEntity living && living.isBlocking()) {
            return Situation.TARGET_BLOCKING;
        }
        if (shieldBrokenTicks > 0 && airborne) {
            return Situation.SHIELD_BROKEN_AIR;
        }
        if (airborne) {
            if (inFront != null && player.fallDistance >= FALL_DISTANCE) {
                return Situation.FALLING;
            }
            if (inFront != null) {
                return Situation.AIRBORNE;
            }
        }
        if (aimed instanceof LivingEntity living) {
            // 够不着的情况必须排在「有甲/无甲」前面，否则 4 格外的目标会去拿打不到的锤子。
            if (player.distanceTo(living) > FAR_DISTANCE) {
                return Situation.TARGET_FAR;
            }
            return living.getArmorValue() > 0 ? Situation.TARGET_ARMORED : Situation.TARGET_UNARMORED;
        }
        return Situation.FALLBACK;
    }

    public static String serializeScores() {
        return serialize(SCORES, Situation.values());
    }

    public static boolean parseScores(String raw) {
        return parse(raw, SCORES, Situation.values(), Situation.class);
    }

    public static String serializeWeights() {
        return serialize(WEIGHTS, Kind.values());
    }

    public static boolean parseWeights(String raw) {
        return parse(raw, WEIGHTS, Kind.values(), Kind.class);
    }

    private static <E extends Enum<E>> String serialize(int[][] table, E[] rows) {
        StringBuilder sb = new StringBuilder();
        for (E row : rows) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(row.name()).append(':');
            int[] values = table[row.ordinal()];
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(values[i]);
            }
        }
        return sb.toString();
    }

    /** 解析失败整表回退默认，绝不半生效。 */
    private static <E extends Enum<E>> boolean parse(String raw, int[][] table, E[] rows, Class<E> type) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        int[][] parsed = new int[table.length][];
        try {
            for (String chunk : raw.split(";")) {
                if (chunk.isBlank()) {
                    continue;
                }
                int colon = chunk.indexOf(':');
                if (colon <= 0) {
                    return false;
                }
                E row = Enum.valueOf(type, chunk.substring(0, colon));
                String[] parts = chunk.substring(colon + 1).split(",");
                if (parts.length != table[row.ordinal()].length) {
                    return false;
                }
                int[] values = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    values[i] = Integer.parseInt(parts[i].trim());
                }
                parsed[row.ordinal()] = values;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        for (int[] values : parsed) {
            if (values == null) {
                return false;
            }
        }
        for (int i = 0; i < table.length; i++) {
            System.arraycopy(parsed[i], 0, table[i], 0, table[i].length);
        }
        return true;
    }
}
