// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import ztqjntm.update.UpdateFt;
import ztqjntm.update.effect.BeeArrowEffect;
import ztqjntm.update.effect.ExplosiveEffect;
import ztqjntm.update.effect.FireArrowEffect;
import ztqjntm.update.effect.FrostArrowEffect;
import ztqjntm.update.effect.GrappleArrowEffect;
import ztqjntm.update.effect.LightningEffect;
import ztqjntm.update.effect.NoneEffect;
import ztqjntm.update.effect.RedstonePulseEffect;
import ztqjntm.update.effect.TeleportEffect;
import ztqjntm.update.effect.WebArrowEffect;
import ztqjntm.update.effect.WindArrowEffect;
import ztqjntm.update.item.quiver.ModComponents;
import ztqjntm.update.item.quiver.QuiverContents;

import java.util.List;

import java.util.function.Function;

public class ModItems {
    public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, UpdateFt.id(name));
        T item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

    // 参数依次是：物品 ID、伤害加成、命中效果、悬停介绍的语言文件 key（每个 key 一行）
    public static final ModArrowItem STONE_ARROW = register("stone_arrow",
            props -> new ModArrowItem(props, 0, NoneEffect.INSTANCE,
                    "tooltip.update-ft.stone_arrow.1"),
            new Item.Properties());

    public static final ModArrowItem COPPER_ARROW = register("copper_arrow",
            props -> new ModArrowItem(props, 0, LightningEffect.INSTANCE,
                    "tooltip.update-ft.copper_arrow.1",
                    "tooltip.update-ft.copper_arrow.2"),
            new Item.Properties());

    public static final ModArrowItem IRON_ARROW = register("iron_arrow",
            props -> new ModArrowItem(props, 1, NoneEffect.INSTANCE,
                    "tooltip.update-ft.iron_arrow.1"),
            new Item.Properties());

    public static final ModArrowItem DIAMOND_ARROW = register("diamond_arrow",
            props -> new ModArrowItem(props, 2, NoneEffect.INSTANCE,
                    "tooltip.update-ft.diamond_arrow.1"),
            new Item.Properties());

    public static final ModArrowItem GOLD_ARROW = register("gold_arrow",
            props -> new ModArrowItem(props, -1, NoneEffect.INSTANCE,
                    "tooltip.update-ft.gold_arrow.1"),
            new Item.Properties());

    public static final ModArrowItem NETHERITE_ARROW = register("netherite_arrow",
            props -> new ModArrowItem(props, 4, NoneEffect.INSTANCE,
                    "tooltip.update-ft.netherite_arrow.1"),
            new Item.Properties());

    public static final ModArrowItem TNT_ARROW = register("tnt_arrow",
            props -> new ModArrowItem(props, -1, ExplosiveEffect.INSTANCE,
                    "tooltip.update-ft.tnt_arrow.1",
                    "tooltip.update-ft.tnt_arrow.2"),
            new Item.Properties());

    public static final ModArrowItem REDSTONE_ARROW = register("redstone_arrow",
            props -> new ModArrowItem(props, -1, RedstonePulseEffect.INSTANCE,
                    "tooltip.update-ft.redstone_arrow.1",
                    "tooltip.update-ft.redstone_arrow.2"),
            new Item.Properties());

    public static final ModArrowItem PEARL_ARROW = register("pearl_arrow",
            props -> new ModArrowItem(props, 0, TeleportEffect.INSTANCE,
                    "tooltip.update-ft.pearl_arrow.1",
                    "tooltip.update-ft.pearl_arrow.2"),
            new Item.Properties());

    // 1.0.2 六支功能箭：统一 -1 伤害，用伤害换效果
    public static final ModArrowItem WEB_ARROW = register("web_arrow",
            props -> new ModArrowItem(props, -1, WebArrowEffect.INSTANCE,
                    "tooltip.update-ft.web_arrow.1",
                    "tooltip.update-ft.web_arrow.2"),
            new Item.Properties());

    public static final ModArrowItem FIRE_ARROW = register("fire_arrow",
            props -> new ModArrowItem(props, -1, FireArrowEffect.INSTANCE,
                    "tooltip.update-ft.fire_arrow.1",
                    "tooltip.update-ft.fire_arrow.2"),
            new Item.Properties());

    public static final ModArrowItem BEE_ARROW = register("bee_arrow",
            props -> new ModArrowItem(props, -1, BeeArrowEffect.INSTANCE,
                    "tooltip.update-ft.bee_arrow.1",
                    "tooltip.update-ft.bee_arrow.2"),
            new Item.Properties());

    public static final ModArrowItem FROST_ARROW = register("frost_arrow",
            props -> new ModArrowItem(props, -1, FrostArrowEffect.INSTANCE,
                    "tooltip.update-ft.frost_arrow.1",
                    "tooltip.update-ft.frost_arrow.2"),
            new Item.Properties());

    public static final ModArrowItem WIND_CHARGE_ARROW = register("wind_charge_arrow",
            props -> new ModArrowItem(props, -1, WindArrowEffect.INSTANCE,
                    "tooltip.update-ft.wind_charge_arrow.1",
                    "tooltip.update-ft.wind_charge_arrow.2"),
            new Item.Properties());

    public static final ModArrowItem GRAPPLE_ARROW = register("grapple_arrow",
            props -> new ModArrowItem(props, -1, GrappleArrowEffect.INSTANCE,
                    "tooltip.update-ft.grapple_arrow.1",
                    "tooltip.update-ft.grapple_arrow.2"),
            new Item.Properties());

    /**
     * 箭袋。
     * <p>
     * 必须 {@code stacksTo(1)}：内容不同的两个箭袋不能叠，写成 1 比“靠组件比较”更明确。
     * 默认组件给一份空内容，这样 tooltip / 界面 / 模型都不必到处判 null。
     * <p>
     * **不要**把它注册进 {@code #minecraft:arrows}，也不要给它注册发射器弹射行为——
     * 否则弓会把箭袋当成弹药射出去。
     */
    public static final QuiverItem QUIVER = register("quiver",
            QuiverItem::new,
            new Item.Properties()
                    .stacksTo(1)
                    .component(ModComponents.QUIVER_CONTENTS, QuiverContents.EMPTY)
                    .component(ModComponents.QUIVER_STATE, 0));

    /** 15 种箭，发射器注册和创造页签共用一份清单 */
    private static final List<Item> ALL_ARROWS = List.of(
            STONE_ARROW, COPPER_ARROW, IRON_ARROW, DIAMOND_ARROW, GOLD_ARROW,
            NETHERITE_ARROW, TNT_ARROW, REDSTONE_ARROW, PEARL_ARROW, WEB_ARROW,
            FIRE_ARROW, BEE_ARROW, FROST_ARROW, WIND_CHARGE_ARROW, GRAPPLE_ARROW);

    public static void initialize() {
        // 发射器默认只注册了原版三种箭；不注册的话模组箭会被当成普通物品吐在地上，
        // AbstractArrowMixin 和无主箭分支都不会触发。
        for (Item arrow : ALL_ARROWS) {
            DispenserBlock.registerProjectileBehavior(arrow);
        }

        // 排到原版箭后面，而不是全部追加到页签末尾（那样会离原版箭很远）
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT)
                .register((itemGroup) -> itemGroup.addAfter(Items.ARROW,
                        ModItems.STONE_ARROW,
                        ModItems.COPPER_ARROW,
                        ModItems.IRON_ARROW,
                        ModItems.DIAMOND_ARROW,
                        ModItems.GOLD_ARROW,
                        ModItems.NETHERITE_ARROW,
                        ModItems.TNT_ARROW,
                        ModItems.REDSTONE_ARROW,
                        ModItems.PEARL_ARROW,
                        ModItems.WEB_ARROW,
                        ModItems.FIRE_ARROW,
                        ModItems.BEE_ARROW,
                        ModItems.FROST_ARROW,
                        ModItems.WIND_CHARGE_ARROW,
                        ModItems.GRAPPLE_ARROW,
                        ModItems.QUIVER
                ));
    }
}
