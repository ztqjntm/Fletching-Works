// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.trade;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import ztqjntm.update.item.ModItems;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 制箭师（Fletcher）的新增交易。
 * <p>
 * 只用 Fabric 的 {@link TradeOfferHelper} 往原版列表末尾追加，原有的“木棍换绿宝石”“绿宝石换箭”
 * “燧石换绿宝石”等一条都不动；helper 会同时写进 {@code TRADES} 和实验性交易重平衡用的
 * {@code EXPERIMENTAL_TRADES}，所以开不开 {@code trade_rebalance} 数据包都能看到。
 * <p>
 * 定价思路：一律**略贵于自己合成**。制箭台一组配方出 2–4 支（TNT 箭 1 支），材料是
 * “箭头材料 + 木棍 + 羽毛”，所以这些交易只解决“懒得 farm / 临时补货”，不是更便宜的获取途径。
 * 等级按原版节奏铺开：新手卖基础箭，学徒卖 TNT / 铁箭，老手才卖元素箭和珍珠箭。
 */
public final class FletcherTrades {

    /** 原版卖箭也用 0.05，需求上涨时加价很温和 */
    private static final float PRICE_MULTIPLIER = 0.05F;

    /** 幂等：初始化只该跑一次；测试里重复调用也不会把交易挂两遍 */
    private static boolean registered;

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        // 新手：基础弹药。石箭伤害与原版相同，但 1 绿宝石只给 8 支（原版 16 支）；铜箭一组只出 4 支
        addOffers(1, offers -> {
            offers.add(sell(1, ModItems.STONE_ARROW, 8, 16, 1));
            offers.add(sell(1, ModItems.COPPER_ARROW, 4, 12, 1));
        });

        // 学徒：TNT 箭与铁箭。2 绿宝石换 2 支 TNT 箭 ≈ 1 绿宝石 1 支，
        // 合成一支要一整块 TNT（5 火药），买只能算省事，谈不上赚。
        addOffers(2, offers -> {
            offers.add(sell(2, ModItems.TNT_ARROW, 2, 6, 5));
            offers.add(sell(2, ModItems.IRON_ARROW, 4, 8, 5));
        });

        // 老手：功能箭。红石 / 冰冻 / 珍珠的合成材料都不贵或可替代，这里刻意把单价抬高
        addOffers(3, offers -> {
            offers.add(sell(2, ModItems.REDSTONE_ARROW, 3, 8, 10));
            offers.add(sell(3, ModItems.FROST_ARROW, 2, 6, 10));
            offers.add(sell(4, ModItems.PEARL_ARROW, 2, 4, 10));
        });
    }

    /**
     * 往指定等级追加。Fabric 会把同一次注册分别交给 {@code TRADES} 和
     * {@code EXPERIMENTAL_TRADES}——而实验表里没有制箭师时，Fabric 的
     * {@code initVillagerTrades} 会把 {@code TRADES} 的内层 map 按引用复制过去，
     * 两张表就成了同一份数据；此时第二次追加必须跳过，否则每条交易都会翻倍。
     */
    private static void addOffers(int level,
                                  Consumer<List<VillagerTrades.ItemListing>> additions) {
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FLETCHER, level,
                (offers, experimental) -> {
                    if (experimental && sharesFletcherTable()) {
                        return;
                    }
                    additions.accept(offers);
                });
    }

    /** 实验性交易表与普通表是否共用同一个制箭师 map（见 {@link #addOffers}） */
    private static boolean sharesFletcherTable() {
        return VillagerTrades.TRADES.get(VillagerProfession.FLETCHER)
                == VillagerTrades.EXPERIMENTAL_TRADES.get(VillagerProfession.FLETCHER);
    }

    /**
     * 一条“绿宝石 → 箭”的交易。只收绿宝石，不搭配第二件材料，
     * 免得玩家为了买箭还得先留一组燧石/羽毛在身上。
     */
    private static VillagerTrades.ItemListing sell(int emeralds, Item arrow, int count,
                                                   int maxUses, int xp) {
        return (level, entity, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, emeralds),
                Optional.empty(),
                new ItemStack(arrow, count),
                maxUses,
                xp,
                PRICE_MULTIPLIER
        );
    }

    private FletcherTrades() {
    }
}
