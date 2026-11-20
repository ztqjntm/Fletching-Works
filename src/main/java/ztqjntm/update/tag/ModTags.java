// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ztqjntm.update.UpdateFt;

/**
 * 本模组用到的物品标签。
 */
public final class ModTags {

    /**
     * 能放进箭头格的物品。
     * <p>
     * 定义在 {@code data/update-ft/tags/item/fletching_tips.json}，数据包可以增删（支持嵌套标签，
     * 例如把 {@code #minecraft:planks} 写进 values），改完 {@code /reload} 即生效，不用改代码。
     * 客户端只查这个标签，不需要额外的配方缓存——标签本身会同步到客户端。
     */
    public static final TagKey<Item> FLETCHING_TIPS =
            TagKey.create(Registries.ITEM, UpdateFt.id("fletching_tips"));

    /**
     * 能放进中间格（杆）的物品，默认只有原版木棍，可选并入 {@code #c:rods/wooden}。
     * 定义在 {@code data/update-ft/tags/item/shafts.json}，数据包可扩展。
     */
    public static final TagKey<Item> SHAFTS =
            TagKey.create(Registries.ITEM, UpdateFt.id("shafts"));

    /**
     * 能放进左下格（羽饰）的物品，默认只有原版羽毛，可选并入 {@code #c:feathers}。
     * 定义在 {@code data/update-ft/tags/item/fletchings.json}，数据包可扩展。
     */
    public static final TagKey<Item> FLETCHINGS =
            TagKey.create(Registries.ITEM, UpdateFt.id("fletchings"));

    /** 15 种模组箭，进度判定用（代码里不需要，保留给数据包/其他模组引用） */
    public static final TagKey<Item> ARROWS =
            TagKey.create(Registries.ITEM, UpdateFt.id("arrows"));

    /**
     * 安全版 {@link ItemStack#is(TagKey)}：标签尚未绑定（单测、注册表早期）时按“不匹配”处理。
     * <p>
     * 正常游戏里标签在数据包加载后一定存在，不会走到 catch；这里主要让逻辑在没有数据包的环境
     * 也能返回确定结果，而不是抛 {@code IllegalStateException}。
     */
    public static boolean isIn(ItemStack stack, TagKey<Item> tag) {
        if (stack.isEmpty()) {
            return false;
        }
        try {
            return stack.is(tag);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private ModTags() {
    }
}
