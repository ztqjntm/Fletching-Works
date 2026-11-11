package ztqjntm.update.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 风弹箭：落点 3×3×3 内所有实体被纯击退，不造成伤害、不破坏方块、不触发红石。
 * <p>
 * 不调用 {@code explode()}：即使是 {@code ExplosionInteraction.NONE} 也会走伤害计算，
 * 与现有的“无实体伤害”计算器互相抵消；手写 AABB 遍历是唯一干净的做法。
 * 玩家必须补 {@link ClientboundSetEntityMotionPacket}，否则客户端会用自己的速度覆盖。
 */
public final class WindArrowEffect implements ArrowEffect {
    public static final WindArrowEffect INSTANCE = new WindArrowEffect();

    private static final double RADIUS = 1.5D;
    private static final double STRENGTH = 3.0D;

    private WindArrowEffect() {
    }

    @Override
    public boolean onHitBlock(ServerLevel level, AbstractArrow arrow, BlockHitResult hit) {
        burst(level, arrow, Vec3.atCenterOf(hit.getBlockPos()));
        return true;
    }

    @Override
    public boolean onHitEntity(ServerLevel level, AbstractArrow arrow, EntityHitResult hit) {
        burst(level, arrow, hit.getLocation());
        return true;
    }

    private static void burst(ServerLevel level, AbstractArrow arrow, Vec3 center) {
        AABB box = AABB.ofSize(center, RADIUS * 2.0D, RADIUS * 2.0D, RADIUS * 2.0D);
        for (Entity entity : level.getEntities(arrow, box, e -> e.isAlive() && !e.isSpectator())) {
            Vec3 offset = entity.position().subtract(center);
            double distance = offset.length();
            Vec3 direction = distance > 1.0E-4D
                    ? offset.normalize()
                    : new Vec3(level.getRandom().nextDouble() - 0.5D, 0.5D,
                            level.getRandom().nextDouble() - 0.5D).normalize();
            double strength = Math.max(0.0D, 1.0D - distance / STRENGTH);
            entity.push(direction.x * strength, 0.3D + direction.y * 0.3D, direction.z * strength);
            if (entity instanceof ServerPlayer player) {
                // 客户端默认会用本地速度覆盖，必须显式同步一次服务端速度
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
        }
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
