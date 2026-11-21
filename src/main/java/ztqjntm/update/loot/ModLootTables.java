// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.loot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import ztqjntm.update.enchantment.ModEnchantments;

import java.util.Map;

/**
 * 无限 II / III 的获取途径：要塞箱子与钓鱼宝藏。
 * <p>
 * 只注入四张原版表（{@code source.isBuiltin()}），数据包覆盖过的表交给数据包自己决定，
 * 避免和整合包的战利品修改互相打架。附魔台那条走 {@code #minecraft:in_enchanting_table}
 * 标签，不需要代码。
 * <p>
 * 两个附魔各占一个 pool，所以同一口箱子理论上可以同时出两本（图书馆 0.4%、交叉箱 0.06%、
 * 走廊箱 0.03%），可以接受。
 */
public final class ModLootTables {
    /** 每条表每次判定的独立概率 */
    private record Rates(float infinityII, float infinityIII) {
    }

    private static final Map<ResourceKey<LootTable>, Rates> RATES = Map.of(
            key("gameplay/fishing/treasure"), new Rates(0.02F, 0.005F),
            key("chests/stronghold_corridor"), new Rates(0.03F, 0.01F),
            key("chests/stronghold_crossing"), new Rates(0.04F, 0.015F),
            key("chests/stronghold_library"), new Rates(0.10F, 0.04F)
    );

    private ModLootTables() {
    }

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            // 只改原版表；数据包覆盖过的表交给数据包自己决定
            if (!source.isBuiltin()) {
                return;
            }
            Rates rates = RATES.get(key);
            if (rates == null) {
                return;
            }
            addBook(table, registries, ModEnchantments.INFINITY_II, rates.infinityII());
            addBook(table, registries, ModEnchantments.INFINITY_III, rates.infinityIII());
        });
    }

    private static void addBook(LootTable.Builder table, HolderLookup.Provider registries,
                                ResourceKey<Enchantment> enchantment, float chance) {
        Holder<Enchantment> holder = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(enchantment);

        table.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .apply(new SetEnchantmentsFunction.Builder()
                                .withEnchantment(holder, ConstantValue.exactly(1.0F)))));
    }

    private static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath("minecraft", path));
    }
}
