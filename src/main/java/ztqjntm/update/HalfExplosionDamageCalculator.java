// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import ztqjntm.update.config.ModConfig;

/**
 * TNT 箭的伤害计算：在原版爆炸伤害上乘一个倍率。
 * <p>
 * 倍率来自 {@code config/fletching-works.json} 的 {@code tntArrow.entityDamageMultiplier}，
 * 每次爆炸现读，{@code /fletchingworks reload} 后立即生效。默认 0.5（原行为）。
 * 方块破坏不受这里影响，走 {@link net.minecraft.world.level.ExplosionInteraction}。
 */
public class HalfExplosionDamageCalculator extends ExplosionDamageCalculator {
    public static final HalfExplosionDamageCalculator INSTANCE = new HalfExplosionDamageCalculator();

    @Override
    public float getEntityDamageAmount(Explosion explosion, Entity entity, float amount) {
        float multiplier = (float) ModConfig.get().tntArrow.entityDamageMultiplier;
        return super.getEntityDamageAmount(explosion, entity, amount) * multiplier;
    }
}
