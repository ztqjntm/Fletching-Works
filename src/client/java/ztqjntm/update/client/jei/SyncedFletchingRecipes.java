package ztqjntm.update.client.jei;

import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.minecraft.world.item.crafting.RecipeHolder;
import ztqjntm.update.recipe.FletchingRecipe;
import ztqjntm.update.recipe.ModRecipes;

import java.util.List;

/**
 * 客户端收到的制箭台配方。
 * <p>
 * 1.21.2 起原版不再把配方整份同步给客户端，1.21.11 的 {@code Level#recipeAccess()} 也只暴露
 * 配方属性集和切石机配方。Fabric 提供了可选的配方同步 API：服务端在主初始化器里调用
 * {@code RecipeSynchronization.synchronizeRecipeSerializer(...)} 之后，客户端会通过
 * {@link net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent}
 * 收到这些序列化器产出的配方。
 * <p>
 * JEI 的 {@code registerRecipes} 在客户端执行，拿不到 {@code RecipeManager}，
 * 所以这里把同步事件里的结果缓存下来给插件用。配方为空时 JEI 只是暂时没有条目，
 * 下次同步（进入世界 / {@code /reload}）会重新填上。
 */
public final class SyncedFletchingRecipes {
    private static volatile List<RecipeHolder<FletchingRecipe>> recipes = List.of();

    private SyncedFletchingRecipes() {
    }

    /** 由 {@code UpdateFtClient} 在客户端收到配方同步时调用 */
    public static void accept(SynchronizedRecipes synchronizedRecipes) {
        recipes = List.copyOf(synchronizedRecipes.getAllOfType(ModRecipes.FLETCHING));
    }

    /** 断开连接时清掉，避免下一个世界在同步完成前显示上一个服务器的配方 */
    public static void clear() {
        recipes = List.of();
    }

    public static List<RecipeHolder<FletchingRecipe>> get() {
        return recipes;
    }
}
