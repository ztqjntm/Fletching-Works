// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import ztqjntm.update.UpdateFt;

/**
 * 无限 II / III 的注册表 key。
 * <p>
 * 附魔是数据包注册表条目，定义在 {@code data/update-ft/enchantment/*.json}，不需要
 * {@code Registry.register}；这里只存 key，供战利品注入引用。
 */
public final class ModEnchantments {
    /** 特殊箭 20% 不消耗，普通箭 100% 不消耗 */
    public static final ResourceKey<Enchantment> INFINITY_II =
            ResourceKey.create(Registries.ENCHANTMENT, UpdateFt.id("infinity_ii"));

    /** 特殊箭 33% 不消耗，普通箭 100% 不消耗 */
    public static final ResourceKey<Enchantment> INFINITY_III =
            ResourceKey.create(Registries.ENCHANTMENT, UpdateFt.id("infinity_iii"));

    private ModEnchantments() {
    }
}
