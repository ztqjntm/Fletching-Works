package ztqjntm.update.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.animal.bee.Bee;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 蜜蜂箭召唤的蜜蜂的寿命管理。
 * <p>
 * 1.21.11 的 {@code Bee} 没有 1.20 时代的 {@code setLimitedLife}，所以这里自己记一份列表，
 * 到 600 tick（30 秒）就 {@code discard()}；蜜蜂提前死亡/被移除时只从列表里摘掉。
 * <p>
 * 只在服务端线程访问，不需要额外同步。
 */
public final class BeeArrowManager {

    private static final int LIFE_TICKS = 600;
    private static final List<Bee> BEES = new ArrayList<>();

    private BeeArrowManager() {
    }

    public static void track(Bee bee) {
        BEES.add(bee);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BeeArrowManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BEES.clear());
    }

    private static void tick(MinecraftServer server) {
        Iterator<Bee> iterator = BEES.iterator();
        while (iterator.hasNext()) {
            Bee bee = iterator.next();
            if (bee.isRemoved()) {
                iterator.remove();
            } else if (bee.tickCount >= LIFE_TICKS) {
                bee.discard();
                iterator.remove();
            }
        }
    }
}
