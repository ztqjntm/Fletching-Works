// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.menu;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ztqjntm.update.recipe.FletchingRecipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 栏位过滤：木棍只进中间、羽毛只进左下。
 * 其他物品（含箭头材料）会走 {@code #update-ft:fletching_tips} 标签查询，单测环境没有数据包、
 * 标签未绑定，所以这里不覆盖。
 */
class FletchingTableSlotsTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void stickGoesToShaftSlot() {
        assertEquals(FletchingRecipes.SHAFT_SLOT, FletchingTableMenu.inputSlotFor(new ItemStack(Items.STICK)));
    }

    @Test
    void featherGoesToFletchingSlot() {
        assertEquals(FletchingRecipes.FLETCHING_SLOT, FletchingTableMenu.inputSlotFor(new ItemStack(Items.FEATHER)));
    }
}
