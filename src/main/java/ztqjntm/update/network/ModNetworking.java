// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import ztqjntm.update.config.ModConfig;
import ztqjntm.update.item.quiver.QuiverContents;
import ztqjntm.update.item.quiver.QuiverHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 箭袋的两种包：
 * <ul>
 *   <li>C2S {@link QuiverSwitchPayload}：客户端请求切换当前槽；</li>
 *   <li>S2C {@link QuiverSyncPayload}：服务端把权威内容推回去。</li>
 * </ul>
 * 服务端是权威：客户端发来的只是“请求”，这里逐项校验后才改组件。
 */
public final class ModNetworking {

    /** 两次切换的最小间隔，和文档一致：每 50ms 最多一次 */
    private static final long MIN_INTERVAL_MS = 50L;

    /** 每个玩家的上次切换时间；玩家退出时清掉 */
    private static final Map<UUID, Long> LAST_SWITCH = new HashMap<>();

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(QuiverSwitchPayload.TYPE, QuiverSwitchPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(QuiverSyncPayload.TYPE, QuiverSyncPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(QuiverSwitchPayload.TYPE,
                (payload, context) -> handleSwitch(context.player(), payload));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            LAST_SWITCH.remove(handler.getPlayer().getUUID());
            QuiverHelper.clearLock(handler.getPlayer().getUUID());
        });
    }

    /** 把某个槽里的箭袋内容推给玩家；只在服务端调用 */
    public static void sendQuiverSync(ServerPlayer player, int inventorySlot, QuiverContents contents) {
        if (ServerPlayNetworking.canSend(player, QuiverSyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, new QuiverSyncPayload(inventorySlot, contents));
        }
    }

    private static void handleSwitch(ServerPlayer player, QuiverSwitchPayload payload) {
        ModConfig config = ModConfig.get();
        if (!config.quiver.enabled) {
            return;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }

        int slot = payload.inventorySlot();
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return;
        }
        if (payload.target() < 0 || payload.target() >= QuiverContents.SLOT_COUNT) {
            return;
        }

        ItemStack quiver = player.getInventory().getItem(slot);
        if (!QuiverHelper.isQuiver(quiver)) {
            return;
        }

        long now = System.currentTimeMillis();
        long interval = Math.max(MIN_INTERVAL_MS, config.quiver.switchCooldownTicks * 50L);
        Long last = LAST_SWITCH.get(player.getUUID());
        if (last != null && now - last < interval) {
            return;
        }

        QuiverContents contents = QuiverHelper.contents(quiver);
        // 空槽不切：和 V 循环“跳过空槽”的规则保持一致
        if (!contents.isSlotEmpty(payload.target()) && contents.selected() != payload.target()) {
            contents = contents.withSelected(payload.target());
            contents.writeTo(quiver);
            player.getInventory().setChanged();
            LAST_SWITCH.put(player.getUUID(), now);
        }
        // 无论切没切都回一份权威状态：界面开着时背包菜单不再广播，客户端靠它对齐
        sendQuiverSync(player, slot, contents);
    }

    private ModNetworking() {
    }
}
