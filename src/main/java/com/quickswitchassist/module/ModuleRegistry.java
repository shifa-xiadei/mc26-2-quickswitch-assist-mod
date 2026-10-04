package com.quickswitchassist.module;

import com.quickswitchassist.module.impl.SwapAssist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 模块注册表。新增辅助只需要在这里加一行。 */
public final class ModuleRegistry {

    private static final List<CombatModule> MODULES = new ArrayList<>();

    static {
        register(new SwapAssist());
    }

    private ModuleRegistry() {
    }

    public static void register(CombatModule module) {
        MODULES.add(module);
    }

    public static List<CombatModule> all() {
        return Collections.unmodifiableList(MODULES);
    }
}
