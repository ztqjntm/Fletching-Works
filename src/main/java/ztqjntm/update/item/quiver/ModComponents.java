// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.item.quiver;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import ztqjntm.update.UpdateFt;

/**
 * 箭袋用到的两个数据组件。
 * <p>
 * {@link #QUIVER_CONTENTS} 是唯一的事实来源；{@link #QUIVER_STATE} 只是给物品模型用的摘要
 * （1.21 的物品模型只能按组件分支，不能直接读“五个槽里总共还剩几支”）。
 * 两者永远通过 {@link QuiverContents#writeTo} 一起写，不要单独设置其中一个。
 */
public final class ModComponents {

    /**
     * 箭袋内容：6 个槽 + 当前槽。
     * <p>
     * 必须 {@code persistent} + {@code networkSynchronized}：前者负责存档 / 掉落 / 箱子，
     * 后者负责 HUD、界面和联机时其他客户端看到的贴图。
     */
    public static final DataComponentType<QuiverContents> QUIVER_CONTENTS = register("quiver_contents",
            DataComponentType.<QuiverContents>builder()
                    .persistent(QuiverContents.CODEC)
                    .networkSynchronized(QuiverContents.STREAM_CODEC)
                    .build());

    /** 0 = 空，1 = 只剩 1 支，2 = 只剩 2 支，3 = 正常 */
    public static final DataComponentType<Integer> QUIVER_STATE = register("quiver_state",
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, UpdateFt.id(name), type);
    }

    /** 空方法，只为触发静态字段初始化（与 ModItems.initialize 的写法一致） */
    public static void initialize() {
    }

    private ModComponents() {
    }
}
