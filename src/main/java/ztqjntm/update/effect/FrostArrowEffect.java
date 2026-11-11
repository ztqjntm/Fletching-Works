package ztqjntm.update.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 冰冻箭：命中实体冻结 10 秒；命中方块把周围 3×3 的水源冻成霜冰。
 * <p>
 * 只处理水源（{@code isSource()}），不动流动水——否则瀑布会被冻成一堵冰墙。
 * 霜冰的融化交给原版 {@link FrostedIceBlock} 自己处理，不需要计划刻或方块实体。
 * <p>
 * 不消耗箭：命中方块后照常插在地上，可以捡回来。
 */
public final class FrostArrowEffect implements ArrowEffect {
    public static final FrostArrowEffect INSTANCE = new FrostArrowEffect();

    /** 冻结时长（tick），10 秒 */
    private static final int FREEZE_TICKS = 200;

    private FrostArrowEffect() {
    }

    @Override
    public boolean onHitEntity(ServerLevel level, AbstractArrow arrow, EntityHitResult hit) {
        Entity target = hit.getEntity();
        if (target.canFreeze()) {
            // 已经冻得更久就不要缩短
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), FREEZE_TICKS));
        }
        return false;
    }

    @Override
    public boolean onHitBlock(ServerLevel level, AbstractArrow arrow, BlockHitResult hit) {
        BlockPos center = findWaterSurface(level, hit.getBlockPos());
        if (center == null) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) {
                continue;
            }
            level.setBlock(pos,
                    Blocks.FROSTED_ICE.defaultBlockState().setValue(FrostedIceBlock.AGE, 0),
                    Block.UPDATE_ALL);
        }
        return false;
    }

    /**
     * 箭射进水里时会先命中水底的方块，不会命中水本身；所以先看命中格/上方，
     * 再在命中格同一层的 3×3 里找水源（射到岸边的情况），找到后向上找水面，
     * 最多找 3 格，避免从深海海底把水面整片冻住。找不到水就返回 null。
     */
    private static BlockPos findWaterSurface(ServerLevel level, BlockPos hit) {
        if (isSourceWater(level, hit)) {
            return findSurface(level, hit);
        }
        if (isSourceWater(level, hit.above())) {
            return findSurface(level, hit.above());
        }
        for (BlockPos pos : BlockPos.betweenClosed(
                hit.offset(-1, 0, -1), hit.offset(1, 0, 1))) {
            if (isSourceWater(level, pos)) {
                // betweenClosed 复用同一个 MutableBlockPos，返回前必须固定下来
                return findSurface(level, pos.immutable());
            }
        }
        return null;
    }

    private static boolean isSourceWater(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos water) {
        BlockPos result = water;
        for (int i = 0; i < 3; i++) {
            BlockPos above = result.above();
            if (!isSourceWater(level, above)) {
                break;
            }
            result = above;
        }
        return result;
    }
}
