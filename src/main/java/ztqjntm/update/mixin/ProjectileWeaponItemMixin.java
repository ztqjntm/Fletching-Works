// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ztqjntm.update.item.quiver.QuiverAmmo;

/**
 * 消耗接管：{@code useAmmo} 是弓发射和弩装填共同的“这一发扣几支”的判定点。
 * <p>
 * 箭袋交出去的是副本，所以原版在这里对副本的 {@code split} 不会动到组件。我们在这个方法
 * 入口就认出“这一发来自箭袋”，用 {@link QuiverAmmo#consume} 按完全相同的规则算出返回值，
 * 同时**只从箭袋扣箭**：
 * <ul>
 *   <li>{@code crit == true}（多重射击的额外箭）不扣，和原版一致；</li>
 *   <li>无限 II/III 的 {@code ammo_use} 触发返还时 {@code use == 0}，也不扣；</li>
 *   <li>创造模式 {@code hasInfiniteMaterials()} 同理；</li>
 *   <li>原版“扣空后从背包移除同类箭”的分支被彻底绕开，背包不会被误扣。</li>
 * </ul>
 */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {

    @Inject(method = "useAmmo", at = @At("HEAD"), cancellable = true)
    private static void updateft$useQuiverAmmo(ItemStack weapon, ItemStack ammo, LivingEntity shooter,
                                               boolean crit, CallbackInfoReturnable<ItemStack> cir) {
        QuiverAmmo.Pending pending = QuiverAmmo.take(ammo);
        if (pending != null) {
            cir.setReturnValue(QuiverAmmo.consume(weapon, ammo, shooter, crit, pending));
        }
    }
}
