package ztqjntm.update.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 制箭台配方的输入：固定三格（箭头材料 / 木棍 / 羽毛）。
 * <p>
 * 只是一份快照，配方匹配期间不会被并发修改；真正的内容物仍在 {@code Container} 里。
 * 槽位下标与 {@link FletchingRecipes#TIP_SLOT} 等常量一致。
 */
public record FletchingRecipeInput(ItemStack tip, ItemStack shaft, ItemStack fletching)
        implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case 0 -> this.tip;
            case 1 -> this.shaft;
            case 2 -> this.fletching;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 3;
    }
}
