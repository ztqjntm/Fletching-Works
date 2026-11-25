// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.trade;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.junit.jupiter.api.Test;
import ztqjntm.update.GameTestBootstrap;
import ztqjntm.update.item.ModItems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证新交易追加在制箭师各等级列表的末尾、成本 / 产出与设计表一致，且每条只挂一次。
 * <p>
 * 测试环境里 Fabric Loader 是否已经跑过 {@code onInitialize()} 取决于加载顺序，
 * 所以这里不假设“注册前 / 后”的绝对条数，只检查：
 * <ul>
 *   <li>追加量要么是 0（已经注册过），要么正好是 2 / 2 / 3（本次注册），绝不能翻倍；</li>
 *   <li>列表末尾的 2 / 2 / 3 条就是我们的交易。</li>
 * </ul>
 * 原版条目只读取长度，不调用它们的 {@code getOffer}（原版实现需要真实的村民和等级）。
 */
class FletcherTradesTest {

    @Test
    void appendsFletcherTradesWithoutReplacingVanilla() {
        GameTestBootstrap.bootstrap();

        int novice = fletcher(1).length;
        int apprentice = fletcher(2).length;
        int journeyman = fletcher(3).length;

        FletcherTrades.register();
        FletcherTrades.register(); // 幂等：第二次必须是空操作

        assertTrue(fletcher(1).length == novice || fletcher(1).length == novice + 2,
                "novice grew by " + (fletcher(1).length - novice));
        assertTrue(fletcher(2).length == apprentice || fletcher(2).length == apprentice + 2,
                "apprentice grew by " + (fletcher(2).length - apprentice));
        assertTrue(fletcher(3).length == journeyman || fletcher(3).length == journeyman + 3,
                "journeyman grew by " + (fletcher(3).length - journeyman));

        // 从末尾倒着取，不假设原版到底有几条
        int last1 = fletcher(1).length - 1;
        assertOffer(fletcher(1)[last1 - 1], 1, ModItems.STONE_ARROW, 8, 16);
        assertOffer(fletcher(1)[last1], 1, ModItems.COPPER_ARROW, 4, 12);

        int last2 = fletcher(2).length - 1;
        assertOffer(fletcher(2)[last2 - 1], 2, ModItems.TNT_ARROW, 2, 6);
        assertOffer(fletcher(2)[last2], 2, ModItems.IRON_ARROW, 4, 8);

        int last3 = fletcher(3).length - 1;
        assertOffer(fletcher(3)[last3 - 2], 2, ModItems.REDSTONE_ARROW, 3, 8);
        assertOffer(fletcher(3)[last3 - 1], 3, ModItems.FROST_ARROW, 2, 6);
        assertOffer(fletcher(3)[last3], 4, ModItems.PEARL_ARROW, 2, 4);
    }

    private static VillagerTrades.ItemListing[] fletcher(int level) {
        return VillagerTrades.TRADES.get(VillagerProfession.FLETCHER).get(level);
    }

    private static void assertOffer(VillagerTrades.ItemListing listing, int emeralds,
                                    Item arrow, int count, int maxUses) {
        MerchantOffer offer = listing.getOffer((ServerLevel) null, (Entity) null, RandomSource.create());
        assertEquals(Items.EMERALD, offer.getCostA().getItem());
        assertEquals(emeralds, offer.getCostA().getCount());
        assertEquals(arrow, offer.getResult().getItem());
        assertEquals(count, offer.getResult().getCount());
        assertEquals(maxUses, offer.getMaxUses());
    }
}
