package ztqjntm.update.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import ztqjntm.update.recipe.FletchingRecipe;

/**
 * JEI 里的“制箭台”配方分类：三个斜向原料格 + 右侧产物格，和模组界面一致。
 * <p>
 * 原料格用 {@code add(Ingredient)} 而不是逐个 ItemStack，这样标签/多物品配方在 JEI 里
 * 也能正确展开和检索。
 */
public final class FletchingRecipeCategory extends AbstractRecipeCategory<RecipeHolder<FletchingRecipe>> {
    private static final int WIDTH = 104;
    private static final int HEIGHT = 60;

    public FletchingRecipeCategory(IGuiHelper guiHelper) {
        super(FletchingJeiPlugin.FLETCHING,
                Component.translatable("jei.update-ft.fletching"),
                guiHelper.createDrawableItemLike(Blocks.FLETCHING_TABLE),
                WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FletchingRecipe> holder, IFocusGroup focuses) {
        FletchingRecipe recipe = holder.value();
        // 右上箭头材料 → 中间木棍 → 左下羽毛，产物在右侧
        builder.addInputSlot(56, 2).setStandardSlotBackground().add(recipe.tip());
        builder.addInputSlot(38, 20).setStandardSlotBackground().add(Items.STICK);
        builder.addInputSlot(20, 38).setStandardSlotBackground().add(Items.FEATHER);
        builder.addOutputSlot(84, 20).setOutputSlotBackground().add(recipe.result());
    }
}
