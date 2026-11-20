// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.recipe;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配方匹配矩阵：三件套必须齐、木棍和羽毛位置不能错、箭头材料必须匹配。
 */
class FletchingRecipeTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static FletchingRecipe recipe() {
        return new FletchingRecipe(Ingredient.of(Items.FLINT), new ItemStack(Items.ARROW, 4));
    }

    private static FletchingRecipeInput input(ItemStack tip, ItemStack shaft, ItemStack fletching) {
        return new FletchingRecipeInput(tip, shaft, fletching);
    }

    private static ItemStack stack(net.minecraft.world.level.ItemLike item) {
        return new ItemStack(item);
    }

    @Test
    void matchesOnlyWithCorrectCompleteSet() {
        FletchingRecipe recipe = recipe();
        assertTrue(recipe.matches(input(stack(Items.FLINT), stack(Items.STICK), stack(Items.FEATHER)), null));
    }

    @Test
    void rejectsWrongTip() {
        FletchingRecipe recipe = recipe();
        assertFalse(recipe.matches(input(stack(Items.DIRT), stack(Items.STICK), stack(Items.FEATHER)), null));
    }

    @Test
    void rejectsWrongShaft() {
        FletchingRecipe recipe = recipe();
        assertFalse(recipe.matches(input(stack(Items.FLINT), stack(Items.BAMBOO), stack(Items.FEATHER)), null));
    }

    @Test
    void rejectsWrongFletching() {
        FletchingRecipe recipe = recipe();
        assertFalse(recipe.matches(input(stack(Items.FLINT), stack(Items.STICK), stack(Items.PAPER)), null));
    }

    @Test
    void rejectsIncompleteInput() {
        FletchingRecipe recipe = recipe();
        assertFalse(recipe.matches(input(ItemStack.EMPTY, stack(Items.STICK), stack(Items.FEATHER)), null));
        assertFalse(recipe.matches(input(stack(Items.FLINT), ItemStack.EMPTY, stack(Items.FEATHER)), null));
        assertFalse(recipe.matches(input(stack(Items.FLINT), stack(Items.STICK), ItemStack.EMPTY), null));
    }

    @Test
    void assembleReturnsAFreshCopy() {
        FletchingRecipe recipe = recipe();
        ItemStack first = recipe.assemble(input(stack(Items.FLINT), stack(Items.STICK), stack(Items.FEATHER)), null);
        assertEquals(4, first.getCount());

        // 改返回值不能影响配方本体，否则第二次合成会跟着变
        first.setCount(1);
        assertEquals(4, recipe.result().getCount());
    }
}
