// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.advancement;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import ztqjntm.update.UpdateFt;

/**
 * 本模组注册的进度触发器。
 * <p>
 * 必须在数据包加载前注册好，否则 {@code data/update-ft/advancement/...} 里的
 * {@code "trigger": "update-ft:quiver_full"} 会因为找不到触发器而报解析错误。
 */
public final class ModAdvancements {

    public static final QuiverFullTrigger QUIVER_FULL = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            UpdateFt.id("quiver_full"),
            new QuiverFullTrigger());

    /** 空方法，只为触发静态字段初始化 */
    public static void initialize() {
    }

    private ModAdvancements() {
    }
}
