// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import ztqjntm.update.UpdateFt;
import ztqjntm.update.entity.ArrowTypeHolder;
import ztqjntm.update.item.ModItems;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞行中的箭用哪张实体贴图。
 * <p>
 * 原版所有普通箭共用 {@code textures/entity/projectiles/arrow.png}，所以 15 种模组箭飞出去
 * 长得一模一样——那些贴图此前就是“占位”的。这里按“这一发是哪个物品”分流到各自的贴图。
 * <p>
 * **不能用 {@code getPickupItemStackOrigin()}**：那个字段只在服务端存在，客户端箭实体是空栈
 * （药箭能变色是因为它单独同步了颜色）。真正的数据来源是 {@link ArrowTypeHolder}
 * ——{@code AbstractArrowMixin} 往实体数据里同步的物品 ID。
 * <p>
 * 贴图是 32×32、和原版 {@code arrow.png} 同一套 UV：左半是正面、右半是背面（背面在模型里
 * 是镜像采样的，所以入库前已经把左半镜像到了右半）。
 * <p>
 * 15 种箭都有配图；查不到时返回 {@code null}，渲染器继续用原版贴图（例如别的模组射出的箭）。
 */
public final class ArrowFlightTextures {

    private static final Map<Item, Identifier> BY_ITEM = new HashMap<>();

    static {
        put(ModItems.STONE_ARROW, "stone_arrow_flight");
        put(ModItems.COPPER_ARROW, "copper_arrow_flight");
        put(ModItems.IRON_ARROW, "iron_arrow_flight");
        put(ModItems.DIAMOND_ARROW, "diamond_arrow_flight");
        put(ModItems.GOLD_ARROW, "gold_arrow_flight");
        put(ModItems.NETHERITE_ARROW, "netherite_arrow_flight");
        put(ModItems.TNT_ARROW, "tnt_arrow_flight");
        put(ModItems.REDSTONE_ARROW, "redstone_arrow_flight");
        put(ModItems.PEARL_ARROW, "pearl_arrow_flight");
        put(ModItems.WEB_ARROW, "web_arrow_flight");
        put(ModItems.FIRE_ARROW, "fire_arrow_flight");
        put(ModItems.FROST_ARROW, "frost_arrow_flight");
        put(ModItems.WIND_CHARGE_ARROW, "wind_charge_arrow_flight");
        put(ModItems.BEE_ARROW, "bee_arrow_flight");
        put(ModItems.GRAPPLE_ARROW, "grapple_arrow_flight");
    }

    private static void put(Item item, String file) {
        BY_ITEM.put(item, Identifier.fromNamespaceAndPath(UpdateFt.MOD_ID,
                "textures/entity/projectiles/" + file + ".png"));
    }

    /** 没有配图时返回 null，调用方保持原版贴图 */
    public static Identifier textureFor(AbstractArrow arrow) {
        String typeId = ((ArrowTypeHolder) arrow).updateft$getArrowTypeId();
        if (typeId == null || typeId.isEmpty()) {
            return null;
        }
        Identifier id = Identifier.tryParse(typeId);
        if (id == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == null ? null : BY_ITEM.get(item);
    }

    private ArrowFlightTextures() {
    }
}
