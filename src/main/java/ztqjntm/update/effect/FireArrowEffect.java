package ztqjntm.update.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 烈焰箭：命中实体时点燃目标，并在落点周围铺一层 3×3 的火；命中方块时只铺火。
 * <p>
 * 火就是普通火（允许蔓延），这是明确要的效果，不属于失误。下雨/水下/doFireTick=false
 * 时原版自己会限制蔓延，这里不额外判断。
 * <p>
 * 不依赖射手：发射器射出的无主箭照常点火。
 */
public final class FireArrowEffect implements ArrowEffect {
    public static final FireArrowEffect INSTANCE = new FireArrowEffect();

    /** 命中实体时点燃的秒数 */
    private static final int FIRE_SECONDS = 5;

    private FireArrowEffect() {
    }

    @Override
    public boolean onHitBlock(ServerLevel level, AbstractArrow arrow, BlockHitResult hit) {
        ignite(level, hit.getBlockPos());
        return true;
    }

    @Override
    public boolean onHitEntity(ServerLevel level, AbstractArrow arrow, EntityHitResult hit) {
        Entity target = hit.getEntity();
        target.igniteForSeconds(FIRE_SECONDS);
        ignite(level, target.blockPosition());
        return true;
    }

    /** 以 center 为中心，水平 3×3 逐格放火：目标格可替换，且下方是固体 */
    private static void ignite(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockState base = level.getBlockState(pos);
            if (!base.canBeReplaced()) {
                continue;
            }
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            level.setBlock(pos, BaseFireBlock.getState(level, pos), Block.UPDATE_ALL);
        }
    }
}
