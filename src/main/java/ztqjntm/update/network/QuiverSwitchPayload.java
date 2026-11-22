// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ztqjntm.update.UpdateFt;

/**
 * 客户端 → 服务端：把某个背包槽里的箭袋切到第 {@code target} 槽。
 * <p>
 * 两个字段都是服务端必须重新校验的东西：客户端只说“我想切”，切不切由服务端看它自己那份
 * 组件决定。这样即使客户端改包，也最多只能切自己的箭袋，且不会越界。
 *
 * @param inventorySlot 箭袋在玩家背包里的下标（0–40）
 * @param target        目标槽 0–5
 */
public record QuiverSwitchPayload(int inventorySlot, int target) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuiverSwitchPayload> TYPE =
            new CustomPacketPayload.Type<>(UpdateFt.id("quiver_switch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuiverSwitchPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, QuiverSwitchPayload::inventorySlot,
                    ByteBufCodecs.VAR_INT, QuiverSwitchPayload::target,
                    QuiverSwitchPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
