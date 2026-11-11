package ztqjntm.update.block;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import ztqjntm.update.UpdateFt;

import java.util.function.Function;

public class ModBlocks {
    /**
     * 红石箭激发的隐形红石脉冲。
     * 不放进创造模式物品栏，也没有掉落物——它只会由红石箭临时放置在命中面。
     */
    public static final Block REDSTONE_PULSE = register(
            "redstone_pulse",
            RedstonePulseBlock::new,
            BlockBehaviour.Properties.of()
                    .noCollision()
                    .noOcclusion()
                    .replaceable()
                    .instabreak()
    );

    /**
     * 蛛网箭留下的临时蛛网：不放进创造模式物品栏，也没有掉落物。
     * 属性直接复制原版蛛网（强度、声音、无碰撞），可燃性在 {@link #initialize()} 里注册。
     */
    public static final Block TEMPORARY_COBWEB = register(
            "temporary_cobweb",
            TemporaryCobwebBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB)
    );

    public static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> blockFactory,
                                               BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, UpdateFt.id(name));
        T block = blockFactory.apply(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        return block;
    }

    // 和 ModItems.initialize() 一样：空方法，只为触发 static 字段初始化
    public static void initialize() {
        // 临时蛛网和原版蛛网一样可燃；烈焰箭/岩浆可以把它烧掉。
        FlammableBlockRegistry.getDefaultInstance().add(TEMPORARY_COBWEB, 60, 100);
    }
}
