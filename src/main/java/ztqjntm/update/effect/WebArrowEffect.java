package ztqjntm.update.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import ztqjntm.update.block.ModBlocks;

/**
 * 蛛网箭：在命中位置留一张 8 秒后自动消失的蛛网。
 * <p>
 * 只放一格，不做 3×3；优先放目标脚下方块，脚下不可替换（台阶、地毯等）时再试上方一格。
 * 放置成功才消耗这支箭；没地方放就按普通箭处理，插在地上可以捡回。
 * <p>
 * 不依赖射手：发射器射出的无主箭照常放网。
 */
public final class WebArrowEffect implements ArrowEffect {
    public static final WebArrowEffect INSTANCE = new WebArrowEffect();

    /** 蛛网持续时间（tick） */
    private static final int DURATION = 160;

    private WebArrowEffect() {
    }

    @Override
    public boolean onHitBlock(ServerLevel level, AbstractArrow arrow, BlockHitResult hit) {
        return tryPlace(level, hit.getBlockPos().relative(hit.getDirection()));
    }

    @Override
    public boolean onHitEntity(ServerLevel level, AbstractArrow arrow, EntityHitResult hit) {
        Entity target = hit.getEntity();
        BlockPos feet = target.blockPosition();
        return tryPlace(level, feet) || tryPlace(level, feet.above());
    }

    private static boolean tryPlace(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        level.setBlock(pos, ModBlocks.TEMPORARY_COBWEB.defaultBlockState(), Block.UPDATE_ALL);
        level.scheduleTick(pos, ModBlocks.TEMPORARY_COBWEB, DURATION);
        return true;
    }
}
