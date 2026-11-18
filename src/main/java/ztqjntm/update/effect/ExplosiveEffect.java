package ztqjntm.update.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import ztqjntm.update.HalfExplosionDamageCalculator;
import ztqjntm.update.config.ModConfig;

/**
 * TNT 箭：命中方块或实体时爆炸，炸完消失。
 * <p>
 * 半径 4、不点火；实体伤害倍率、是否拆方块、整个效果是否启用都在
 * {@link ModConfig} 里（{@code config/fletching-works.json}）。
 * 方块破坏同时受配置和 {@code mobGriefing} 约束：任一为 false 都不炸方块，只保留伤害。
 */
public final class ExplosiveEffect implements ArrowEffect {
    public static final ExplosiveEffect INSTANCE = new ExplosiveEffect();

    /** 爆炸半径，与原版 TNT 一致 */
    private static final float RADIUS = 4.0F;

    private ExplosiveEffect() {
    }

    @Override
    public boolean onHitBlock(ServerLevel level, AbstractArrow arrow, BlockHitResult hit) {
        return this.explode(level, arrow, hit.getLocation());
    }

    @Override
    public boolean onHitEntity(ServerLevel level, AbstractArrow arrow, EntityHitResult hit) {
        return this.explode(level, arrow, hit.getLocation());
    }

    /**
     * @return 是否消耗这支箭；配置关掉时返回 {@code false}，箭照常插在地上可以捡回
     */
    private boolean explode(ServerLevel level, AbstractArrow arrow, Vec3 pos) {
        ModConfig.TntArrow config = ModConfig.get().tntArrow;
        if (!config.enabled) {
            return false;
        }
        boolean breaksBlocks = config.breaksBlocks && level.getGameRules().get(GameRules.MOB_GRIEFING);
        Level.ExplosionInteraction interaction = breaksBlocks
                ? Level.ExplosionInteraction.TNT
                : Level.ExplosionInteraction.NONE;
        level.explode(
                arrow,
                Explosion.getDefaultDamageSource(level, arrow),
                HalfExplosionDamageCalculator.INSTANCE,
                pos.x,
                pos.y,
                pos.z,
                RADIUS,
                false,
                interaction
        );
        return true;
    }
}
