package ztqjntm.update.recipe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ztqjntm.update.tag.ModTags;

/**
 * 制箭台的栏位约定与配方查询。
 * <p>
 * 栏位是固定的，和箭的构造一一对应：
 * <ul>
 *     <li>{@link #TIP_SLOT}=0（界面右上）——箭头材料，随配方变化；</li>
 *     <li>{@link #SHAFT_SLOT}=1（界面中间）——恒为木棍；</li>
 *     <li>{@link #FLETCHING_SLOT}=2（界面左下）——恒为羽毛。</li>
 * </ul>
 * 配方本体不再是代码里的静态表，而是 {@code data/<namespace>/recipe/*.json} 里的
 * {@code update-ft:fletching} 配方，由 {@link net.minecraft.world.item.crafting.RecipeManager}
 * 在服务端加载、匹配；数据包可以增删改，{@code /reload} 生效。
 * <p>
 * 箭头格“能放什么”和“能做出什么”是两件事：能放什么由物品标签
 * {@link ModTags#FLETCHING_TIPS} 决定（数据包可改，{@code /reload} 生效，且会同步给客户端），
 * 能做出什么由配方表决定。客户端拿不到配方表，所以放置预判只能用标签。
 */
public final class FletchingRecipes {
    public static final int TIP_SLOT = 0;
    public static final int SHAFT_SLOT = 1;
    public static final int FLETCHING_SLOT = 2;

    private FletchingRecipes() {
    }

    /**
     * 这个物品能不能放进箭头格（{@code InputSlot.mayPlace} 与 Shift 点击的路由都用它）。
     * <p>
     * 只查物品标签，不查配方：客户端没有配方表，而标签是原版同步的，
     * 两端看到的是同一份，预判不会和服务端打架。
     */
    public static boolean isTipMaterial(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.FLETCHING_TIPS);
    }

    /**
     * 找出匹配当前三格原料的产物。木棍/羽毛的校验在 {@link FletchingRecipe#matches} 里。
     * <p>
     * 只在服务端查询：1.21.11 的 {@code Level#recipeAccess()} 在客户端不提供配方表，
     * 产物格由服务端算好后随容器同步发给客户端（和原版工作台一致）。
     * 没有匹配则返回空栈。
     */
    public static ItemStack findResult(Level level, Container input) {
        if (!(level instanceof ServerLevel server) || input.getContainerSize() <= FLETCHING_SLOT) {
            return ItemStack.EMPTY;
        }
        FletchingRecipeInput recipeInput = new FletchingRecipeInput(
                input.getItem(TIP_SLOT), input.getItem(SHAFT_SLOT), input.getItem(FLETCHING_SLOT));
        return server.getServer().getRecipeManager()
                .getRecipeFor(ModRecipes.FLETCHING, recipeInput, server)
                .map(holder -> holder.value().assemble(recipeInput, server.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
