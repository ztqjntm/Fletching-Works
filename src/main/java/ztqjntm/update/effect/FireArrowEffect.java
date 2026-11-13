package ztqjntm.update.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 烈焰箭：命中实体时点燃目标，同时在落点周围铺一层 3×3 的火；命中方块时同样铺火。
 * <p>
 * 落点按“命中面外侧”取，射到地面就是地面上方那一层，所以落地一定会烧起来，
 * 不是只有打中生物才有效果。
 * <p>
 * 火就是普通火（允许蔓延），这是明确要的效果。但 {@code mobGriefing=false} 时整片火都不放，
 * 和同包的 TNT 箭保持一致——远程纵火是服务器最容易引起投诉的行为。
 * 点燃实体不受影响（原版火焰箭也照样点燃实体）。
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
        // 以“命中面外侧”那一格为中心：射到地面时是地面上方，射到墙面时是墙边那一格。
        // 直接用 hit.getBlockPos() 会得到脚下那块实体方块，canBePlacedAt 全部为 false，一格火都放不出来。
        BlockPos anchor = hit.getBlockPos().relative(hit.getDirection());
        if (ignite(level, anchor)) {
            return true;
        }
        // 打在悬空方块的侧面/底面时，外侧那一格下面是空的，火站不住；退回命中格上方再试一次。
        // 两处都点不着（含 mobGriefing=false）就不消耗这支箭，让它照常插在地上可以捡回。
        return ignite(level, hit.getBlockPos().above());
    }

    @Override
    public boolean onHitEntity(ServerLevel level, AbstractArrow arrow, EntityHitResult hit) {
        Entity target = hit.getEntity();
        target.igniteForSeconds(FIRE_SECONDS);
        ignite(level, target.blockPosition());
        return true;
    }

    /**
     * 以 center 为中心，水平 3×3 逐格放火。
     * <p>
     * 用 {@link BaseFireBlock#canBePlacedAt} 判定而不是“可替换 + 下方实体”：它同时处理了
     * 空气、火焰可存活、灵魂火和传送门，也顺便挡掉了水（水里不能点火）。
     *
     * @return 是否至少放成功了一格
     */
    private static boolean ignite(ServerLevel level, BlockPos center) {
        // 管理员关了 mobGriefing 就不该被远程纵火。点燃实体在 onHitEntity 里，不受这条影响。
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }
        boolean placed = false;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (!BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
                continue;
            }
            level.setBlock(pos, BaseFireBlock.getState(level, pos), Block.UPDATE_ALL);
            placed = true;
        }
        return placed;
    }
}
