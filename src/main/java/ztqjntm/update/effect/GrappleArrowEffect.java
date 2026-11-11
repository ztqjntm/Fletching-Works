package ztqjntm.update.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import ztqjntm.update.entity.GrappleManager;

/**
 * 钩爪箭：命中方块时把射手拉过去；命中生物时把目标拉向射手，最多 5 格。
 * <p>
 * 方块命中点沿飞行反方向退 0.6 格做锚点，避免把人塞进墙里；超过 32 格不拉。
 * 命中实体时锁定初始方向，不每 tick 重算，否则目标会绕圈；最多拉 5 格，离射手至少留 1 格。
 * <p>
 * 箭本身不消耗：命中方块后照常插在地上，可以捡回来。没有主人（发射器）时不拉、不消耗。
 */
public final class GrappleArrowEffect implements ArrowEffect {
    public static final GrappleArrowEffect INSTANCE = new GrappleArrowEffect();

    private static final double MAX_DISTANCE = 32.0D;
    private static final double BACK_OFF = 0.6D;
    private static final double MAX_PULL = 5.0D;
    private static final double MIN_PULL = 0.5D;

    private GrappleArrowEffect() {
    }

    @Override
    public boolean onHitBlock(ServerLevel level, AbstractArrow arrow, BlockHitResult hit) {
        Entity owner = arrow.getOwner();
        if (owner == null) {
            return false;
        }
        Vec3 anchor = hit.getLocation();
        Vec3 motion = arrow.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-7D) {
            anchor = anchor.subtract(motion.normalize().scale(BACK_OFF));
        }
        if (owner.position().distanceTo(anchor) > MAX_DISTANCE) {
            return false;
        }
        GrappleManager.start(level, owner, anchor);
        return false;
    }

    @Override
    public boolean onHitEntity(ServerLevel level, AbstractArrow arrow, EntityHitResult hit) {
        Entity owner = arrow.getOwner();
        Entity target = hit.getEntity();
        if (owner == null || target == owner || !target.isAlive() || target.isSpectator()) {
            return false;
        }

        Vec3 start = target.position();
        Vec3 toOwner = owner.position().subtract(start);
        double distance = toOwner.length();
        if (distance < 1.0D) {
            return false;
        }
        double pull = Math.min(MAX_PULL, distance - 1.0D);
        if (pull < MIN_PULL) {
            return false;
        }
        GrappleManager.start(level, target, start.add(toOwner.normalize().scale(pull)));
        return false;
    }
}
