// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.menu;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ztqjntm.update.GameTestBootstrap;
import ztqjntm.update.recipe.FletchingRecipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 栏位过滤：木棍只进中间、羽毛只进左下、其他物品哪都不去。
 * 单测环境没有数据包，箭头材料标签未绑定，{@code ModTags.isIn} 会按“不匹配”处理，
 * 所以箭头格的正向路径不在这里覆盖。
 */
class FletchingTableSlotsTest {

    @BeforeAll
    static void bootstrap() {
        GameTestBootstrap.bootstrap();
    }

    @Test
    void stickGoesToShaftSlot() {
        assertEquals(FletchingRecipes.SHAFT_SLOT, FletchingTableMenu.inputSlotFor(new ItemStack(Items.STICK)));
    }

    @Test
    void featherGoesToFletchingSlot() {
        assertEquals(FletchingRecipes.FLETCHING_SLOT, FletchingTableMenu.inputSlotFor(new ItemStack(Items.FEATHER)));
    }

    @Test
    void unrelatedItemGoesNowhere() {
        assertEquals(-1, FletchingTableMenu.inputSlotFor(new ItemStack(Items.DIRT)));
    }
}
