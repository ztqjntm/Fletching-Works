// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ztqjntm.update.UpdateFt;
import ztqjntm.update.item.quiver.QuiverContents;

/**
 * 服务端 → 客户端：某个背包槽里的箭袋内容（含当前槽）变化了。
 * <p>
 * 正常游玩时背包菜单每 tick 会把变化的槽位发下去，不需要这个包；但**打开箭袋界面时**
 * 玩家的当前菜单变成了箭袋菜单，背包菜单不再广播，客户端那份箭袋组件就会停在旧值。
 * 界面里的高亮、以及点三角后的确认都依赖它。
 *
 * @param inventorySlot 箭袋在玩家背包里的下标
 * @param contents      服务端权威内容
 */
public record QuiverSyncPayload(int inventorySlot, QuiverContents contents) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuiverSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(UpdateFt.id("quiver_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuiverSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, QuiverSyncPayload::inventorySlot,
                    QuiverContents.STREAM_CODEC, QuiverSyncPayload::contents,
                    QuiverSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
