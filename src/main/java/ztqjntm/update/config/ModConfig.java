// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 服务端配置：{@code config/fletching-works.json}。
 * <p>
 * 只覆盖“服主真的会想调”的几项：TNT 箭开关/拆不拆方块/伤害倍率、烈焰箭范围与点燃时间、
 * 铜箭是否必须雷雨、钩爪冷却与最大距离、关界面是否退还材料。默认值等于原来的硬编码行为。
 * <p>
 * Gson 反序列化时缺失字段保留默认值，所以旧配置文件缺项不会炸；数值在 {@link #sanitize()} 里钳到
 * 合理范围，避免手改成负数导致火焰半径死循环之类的问题。
 * <p>
 * 改完执行 {@code /fletchingworks reload}（权限等级 2）即可生效，不用重启。
 */
public final class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("FletchingWorks/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "fletching-works.json";

    private static volatile ModConfig instance = new ModConfig();

    public TntArrow tntArrow = new TntArrow();
    public FireArrow fireArrow = new FireArrow();
    public LightningArrow lightningArrow = new LightningArrow();
    public GrappleArrow grappleArrow = new GrappleArrow();
    public Table table = new Table();

    private ModConfig() {
    }

    public static ModConfig get() {
        return instance;
    }

    /** 主初始化器里调用：没有配置文件就生成一份默认的 */
    public static void initialize() {
        load();
    }

    /** 重新读盘；解析失败时保留当前配置，不覆盖玩家的文件 */
    public static void load() {
        Path path = path();
        if (!Files.exists(path)) {
            save(path, instance);
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            ModConfig config = loaded != null ? loaded : new ModConfig();
            config.sanitize();
            instance = config;
        } catch (Exception e) {
            LOGGER.error("Failed to read {}, keeping the current config", path, e);
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static void save(Path path, ModConfig config) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write default config {}", path, e);
        }
    }

    /** 钳掉手改出来的越界值，并补上可能被写成 null 的整段配置 */
    private void sanitize() {
        if (this.tntArrow == null) {
            this.tntArrow = new TntArrow();
        }
        if (this.fireArrow == null) {
            this.fireArrow = new FireArrow();
        }
        if (this.lightningArrow == null) {
            this.lightningArrow = new LightningArrow();
        }
        if (this.grappleArrow == null) {
            this.grappleArrow = new GrappleArrow();
        }
        if (this.table == null) {
            this.table = new Table();
        }

        this.tntArrow.entityDamageMultiplier = clamp(this.tntArrow.entityDamageMultiplier, 0.0D, 10.0D);
        this.fireArrow.spread = (int) clamp(this.fireArrow.spread, 0, 3);
        this.fireArrow.igniteSeconds = (int) clamp(this.fireArrow.igniteSeconds, 0, 60);
        this.grappleArrow.cooldownTicks = (int) clamp(this.grappleArrow.cooldownTicks, 0, 600);
        this.grappleArrow.maxDistance = clamp(this.grappleArrow.maxDistance, 1.0D, 128.0D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** TNT 箭 */
    public static final class TntArrow {
        /** 关掉后 TNT 箭不再爆炸，按普通箭插在地上 */
        public boolean enabled = true;
        /** 是否破坏方块；仍受 {@code mobGriefing} 约束，这里为 true 且 mobGriefing=false 时也不破坏 */
        public boolean breaksBlocks = true;
        /** 实体伤害倍率，0 = 不伤人，1 = 原版 TNT */
        public double entityDamageMultiplier = 0.5D;
    }

    /** 烈焰箭 */
    public static final class FireArrow {
        /** 铺火半径：0 = 单格，1 = 3×3，2 = 5×5，3 = 7×7 */
        public int spread = 1;
        /** 命中实体时点燃的秒数 */
        public int igniteSeconds = 5;
    }

    /** 铜箭 */
    public static final class LightningArrow {
        /** true = 只有雷雨天才能引雷（默认）；false = 晴天也能引雷 */
        public boolean requiresThunderstorm = true;
    }

    /** 钩爪箭 */
    public static final class GrappleArrow {
        /** 成功牵引后的冷却 tick，0 = 无冷却 */
        public int cooldownTicks = 30;
        /** 最大牵引距离（方块） */
        public double maxDistance = 32.0D;
    }

    /** 制箭台 */
    public static final class Table {
        /** 关界面时是否把材料退回玩家；false = 留在桌子里，下次打开还在 */
        public boolean returnItemsOnClose = true;
    }
}
