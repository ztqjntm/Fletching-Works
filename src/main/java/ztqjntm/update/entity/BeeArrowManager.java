package ztqjntm.update.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.bee.Bee;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 蜜蜂箭召唤的蜜蜂的寿命管理。
 * <p>
 * 1.21.11 的 {@code Bee} 没有 1.20 时代的 {@code setLimitedLife}，所以这里自己记一份列表，
 * 30 秒后 {@code discard()}；蜜蜂提前死亡/被移除时只从列表里摘掉。
 * <p>
 * 计时用**服务端 tick**（{@code server.getTickCount()}）而不是 {@code bee.tickCount}：
 * 实体 tick 只在区块加载时推进，射到远处、区块卸载后蜜蜂不会消失，而是留在存档里等下次加载补算。
 * 服务端 tick 一直在走，区块卸载期间也会正常到期。
 * <p>
 * 只在服务端线程访问，不需要额外同步。
 */
public final class BeeArrowManager {

    private static final int LIFE_TICKS = 600;
    /** 召唤时记下“到哪一 tick 该散场”，不依赖实体的 tickCount */
    private record Tracked(Bee bee, int expireAt) {
    }

    private static final List<Tracked> BEES = new ArrayList<>();

    private BeeArrowManager() {
    }

    public static void track(Bee bee) {
        if (bee.level() instanceof ServerLevel level && level.getServer() != null) {
            BEES.add(new Tracked(bee, level.getServer().getTickCount() + LIFE_TICKS));
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BeeArrowManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BEES.clear());
    }

    private static void tick(MinecraftServer server) {
        Iterator<Tracked> iterator = BEES.iterator();
        while (iterator.hasNext()) {
            Tracked tracked = iterator.next();
            Bee bee = tracked.bee();
            if (bee.isRemoved()) {
                iterator.remove();
            } else if (server.getTickCount() >= tracked.expireAt()) {
                bee.discard();
                iterator.remove();
            }
        }
    }
}
