package com.quickswitchassist.config;

import com.quickswitchassist.Edition;
import com.quickswitchassist.module.rule.PriorityTable;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class CombatConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("quickswitch-assist");
    private static final String FILE_NAME = "quickswitch-assist.properties";

    public static boolean enabled = true;
    public static boolean debugHud = Edition.DEV;

    /** 诊断日志。默认关，需要时在 ModMenu 里打开（用户版恒关，QuickSwitchAssistClient.debug 会兜底检查 Edition.DEV）。 */
    public static boolean debugLog = false;

    /** 秒切总开关。 */
    public static boolean swapEnabled = true;

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            LOGGER.info("[quickswitch-assist] no config yet, writing defaults");
            save();
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            LOGGER.warn("[quickswitch-assist] failed to read config, using defaults: {}", e.toString());
            return;
        }

        enabled = readBool(props, "enabled", enabled);
        debugHud = readBool(props, "debugHud", debugHud);
        debugLog = readBool(props, "debugLog", debugLog);
        swapEnabled = readBool(props, "swap.enabled", swapEnabled);

        String scores = props.getProperty("swap.priority", "");
        String weights = props.getProperty("swap.fromWeights", "");
        boolean bad = false;
        if (scores.isBlank()) {
            PriorityTable.resetToDefaults();
        } else if (!PriorityTable.parseScores(scores)) {
            PriorityTable.resetToDefaults();
            bad = true;
        }
        if (!weights.isBlank() && !PriorityTable.parseWeights(weights)) {
            PriorityTable.resetToDefaults();
            bad = true;
        }
        if (bad) {
            LOGGER.warn("[quickswitch-assist] 优先级表解析失败，已回退默认表");
        }

        LOGGER.info("[quickswitch-assist] config loaded: enabled={} hud={} swap={}",
                enabled, debugHud, swapEnabled);

        save();
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("enabled", String.valueOf(enabled));
        props.setProperty("debugHud", String.valueOf(debugHud));
        props.setProperty("debugLog", String.valueOf(debugLog));
        props.setProperty("swap.enabled", String.valueOf(swapEnabled));
        props.setProperty("swap.priority", PriorityTable.serializeScores());
        props.setProperty("swap.fromWeights", PriorityTable.serializeWeights());
        try (OutputStream out = Files.newOutputStream(configPath())) {
            props.store(out, "quickswitch-assist - client-side combat assist");
        } catch (IOException e) {
            LOGGER.warn("[quickswitch-assist] failed to write config: {}", e.toString());
        }
    }

    private static boolean readBool(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw);
    }
}
