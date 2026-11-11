package ztqjntm.update.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 一条制箭台配方（数据包驱动，放在 {@code data/<namespace>/recipe/*.json}）。
 * <p>
 * 只描述“箭头材料 → 产物”，木棍和羽毛是固定栏位，在 {@link #matches} 里直接校验：
 * <pre>{@code
 * {
 *   "type": "update-ft:fletching",
 *   "tip": "minecraft:flint",
 *   "result": { "id": "minecraft:arrow", "count": 4 }
 * }
 * }</pre>
 * {@code tip} 用的是原版 {@link Ingredient#CODEC}，所以也支持物品标签（{@code "#minecraft:planks"}）
 * 和物品列表。
 * <p>
 * 注意：箭头格**能不能放**由物品标签 {@code update-ft:fletching_tips} 决定——客户端拿不到配方表，
 * 只能用同步过去的标签做预判。加一种新箭头材料时，配方和标签都要改。
 */
public record FletchingRecipe(Ingredient tip, ItemStack result) implements Recipe<FletchingRecipeInput> {

    @Override
    public boolean matches(FletchingRecipeInput input, Level level) {
        return input.shaft().is(Items.STICK)
                && input.fletching().is(Items.FEATHER)
                && this.tip.test(input.tip());
    }

    @Override
    public ItemStack assemble(FletchingRecipeInput input, HolderLookup.Provider registries) {
        // 每次都给一份新的，避免外部改到配方本体
        return this.result.copy();
    }

    @Override
    public RecipeSerializer<? extends Recipe<FletchingRecipeInput>> getSerializer() {
        return ModRecipes.FLETCHING_SERIALIZER;
    }

    @Override
    public RecipeType<? extends Recipe<FletchingRecipeInput>> getType() {
        return ModRecipes.FLETCHING;
    }

    /**
     * 三个固定栏位（箭头材料 / 木棍 / 羽毛），下标与菜单槽位一致。
     * <p>
     * 不能返回 {@link PlacementInfo#NOT_PLACEABLE}：{@code RecipeManager} 会把空配方的配方
     * 从展示数据里剔除，并在启动日志里刷一排 “can't be placed … will be ignored”。
     */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(List.of(this.tip, Ingredient.of(Items.STICK), Ingredient.of(Items.FEATHER)));
    }

    /** 界面没有配方书按钮，这个分类不会被实际使用，给一个原版条目满足接口即可 */
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    /** JSON 与网络同步用的编解码器 */
    @SuppressWarnings("deprecation") // 1.21.11 把 streamCodec 标成弃用，但接口里仍是抽象方法，必须实现
    public static final class Serializer implements RecipeSerializer<FletchingRecipe> {
        public static final MapCodec<FletchingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Ingredient.CODEC.fieldOf("tip").forGetter(FletchingRecipe::tip),
                        ItemStack.CODEC.fieldOf("result").forGetter(FletchingRecipe::result))
                .apply(instance, FletchingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FletchingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, FletchingRecipe::tip,
                        ItemStack.STREAM_CODEC, FletchingRecipe::result,
                        FletchingRecipe::new);

        @Override
        public MapCodec<FletchingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FletchingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
