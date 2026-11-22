// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ztqjntm.update.config.ModConfig;
import ztqjntm.update.item.quiver.QuiverHelper;

/**
 * 弹药收口：整个箭袋只需要接进 {@link Player#getProjectile(ItemStack)} 这一个方法。
 * <p>
 * 原版弓、弩在三个时刻都会问“这个玩家有什么箭”——开始拉弓、松手发射、弩装填——三处都走这里，
 * 所以在这里返回箭袋当前槽的箭，三个时刻自动一致，不会出现“能拉弓但射不出去”的空放。
 * <p>
 * 优先级严格按 {@code 副手箭 > 箭袋当前槽 > 背包箭}：
 * <ul>
 *   <li>先跑原版逻辑（它已经处理了手持弹药和背包扫描），在 RETURN 处再判断；</li>
 *   <li>手持弹药非空时直接放行，不压掉“把箭放手上一发就用这种”的原版习惯；</li>
 *   <li>箭袋为空时也放行，让原版结果（包括创造模式的默认箭）原样返回。</li>
 * </ul>
 * 交出去的是**副本**，扣箭由 {@code ProjectileWeaponItemMixin} 在 draw 之后按实际消耗结算。
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void updateft$quiverAmmo(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        if (!ModConfig.get().quiver.enabled) {
            return;
        }
        if (!(weapon.getItem() instanceof ProjectileWeaponItem projectileWeapon)) {
            return;
        }

        Player self = (Player) (Object) this;
        // 副手（或另一只手）已经拿着箭：这一发就用它，箭袋不参与
        if (!ProjectileWeaponItem.getHeldProjectile(self, projectileWeapon.getSupportedHeldProjectiles()).isEmpty()) {
            return;
        }

        ItemStack ammo = QuiverHelper.pickForWeapon(self, projectileWeapon);
        if (!ammo.isEmpty()) {
            cir.setReturnValue(ammo);
        }
    }
}
