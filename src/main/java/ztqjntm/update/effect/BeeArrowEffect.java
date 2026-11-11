package ztqjntm.update.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import ztqjntm.update.entity.BeeArrowManager;

/**
 * 蜜蜂箭：命中实体时召唤 2 只只攻击该目标的蜜蜂，30 秒后消失。
 * <p>
 * 命中方块不生成（没有目标，蜜蜂会转而蜇附近的人）；发射器射出的无主箭也不生成。
 * 无生命目标（盔甲架、船、画）同样跳过。
 * <p>
 * 蜜蜂蜇完会死是原版机制，所以“2 只蜜蜂”实际是 2 次蜇。
 */
public final class BeeArrowEffect implements ArrowEffect {
    public static final BeeArrowEffect INSTANCE = new BeeArrowEffect();

    private static final int BEE_COUNT = 2;
    private static final int ANGRY_TICKS = 600;

    private BeeArrowEffect() {
    }

    @Override
    public boolean onHitEntity(ServerLevel level, AbstractArrow arrow, EntityHitResult hit) {
        if (arrow.getOwner() == null) {
            return false;
        }
        if (!(hit.getEntity() instanceof LivingEntity target)
                || !target.isAlive()
                || target instanceof ArmorStand) {
            return false;
        }

        boolean spawned = false;
        for (int i = 0; i < BEE_COUNT; i++) {
            Bee bee = EntityType.BEE.create(level, EntitySpawnReason.TRIGGERED);
            if (bee == null) {
                continue;
            }
            Vec3 pos = findSpawn(level, bee, target, i);
            bee.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
            bee.setTarget(target);
            bee.setPersistentAngerTarget(EntityReference.of(target.getUUID()));
            bee.setTimeToRemainAngry(ANGRY_TICKS);
            level.addFreshEntity(bee);
            BeeArrowManager.track(bee);
            spawned = true;
        }
        return spawned;
    }

    /** 在目标周围 1.2 格找 8 个方向，优先不卡方块的位置；都不行就在目标脚下生成 */
    private static Vec3 findSpawn(ServerLevel level, Bee bee, LivingEntity target, int index) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = (Math.PI * 2.0D / 8.0D) * ((attempt + index * 4) & 7);
            double x = target.getX() + Math.cos(angle) * 1.2D;
            double z = target.getZ() + Math.sin(angle) * 1.2D;
            double y = target.getY() + 0.2D;
            bee.snapTo(x, y, z, 0.0F, 0.0F);
            if (level.noCollision(bee, bee.getBoundingBox())) {
                return new Vec3(x, y, z);
            }
        }
        return target.position();
    }
}
