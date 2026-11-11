package ztqjntm.update.recipe;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import ztqjntm.update.UpdateFt;

/**
 * 制箭台配方类型与序列化器的注册。
 * <p>
 * 类型/序列化器都只注册一次，配方本体来自数据包（{@code data/update-ft/recipe/*.json}），
 * 可以用数据包增删改，{@code /reload} 生效。
 */
public final class ModRecipes {
    /** 配方类型：{@code update-ft:fletching} */
    public static final RecipeType<FletchingRecipe> FLETCHING = new RecipeType<>() {
        @Override
        public String toString() {
            return "update-ft:fletching";
        }
    };

    /** 配方序列化器：负责 JSON 与网络同步的编解码 */
    public static final RecipeSerializer<FletchingRecipe> FLETCHING_SERIALIZER = new FletchingRecipe.Serializer();

    private ModRecipes() {
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.RECIPE_TYPE, UpdateFt.id("fletching"), FLETCHING);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, UpdateFt.id("fletching"), FLETCHING_SERIALIZER);
    }
}
