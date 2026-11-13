package ztqjntm.update.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import ztqjntm.update.recipe.FletchingRecipe;
import ztqjntm.update.recipe.ModRecipes;

/**
 * JEI 兼容插件。
 * <p>
 * Fabric 下 JEI 通过 {@code jei_mod_plugin} 入口点发现插件（见 {@code fabric.mod.json}），
 * 类上仍需 {@link JeiPlugin} 注解；插件类必须有无参构造器。
 * <p>
 * 配方来源：客户端没有 {@code RecipeManager}，配方由 Fabric 的配方同步 API 送到客户端，
 * {@link SyncedFletchingRecipes} 缓存了这份数据，这里直接交给 JEI。
 */
@JeiPlugin
public final class FletchingJeiPlugin implements IModPlugin {
    /** JEI 侧的配方类型；用 {@link IRecipeHolderType} 保留配方 id，JEI 的书签/查找都依赖它 */
    public static final IRecipeHolderType<FletchingRecipe> FLETCHING =
            IRecipeHolderType.create(ModRecipes.FLETCHING);

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("update-ft", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new FletchingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FLETCHING, SyncedFletchingRecipes.get());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 右键制箭台就能看到这一页
        registration.addCraftingStation(FLETCHING, Blocks.FLETCHING_TABLE);
    }
}
