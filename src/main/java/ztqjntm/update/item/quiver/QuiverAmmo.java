// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.item.quiver;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 把“这一发用的是箭袋里的哪一槽”从 {@code getProjectile} 带到真正扣箭的时刻。
 * <p>
 * 为什么绕这一圈：箭袋的内容存在数据组件里，组件里的 {@link ItemStack} 不能直接交给原版去
 * {@code shrink}——原地改不会触发组件同步与存档。所以 {@code getProjectile} 交出去的是**副本**，
 * 原版照常对副本结算；等 {@code useAmmo} 决定“这一发扣几支”时，我们接管这次结算：
 * <b>从箭袋扣，不碰背包</b>。
 * <p>
 * 之前是在 {@code draw} 返回时按“副本少了几支”反推扣箭。那条路一旦副本与背包里的同类箭搞混
 * （原版 {@code useAmmo} 在副本扣空时还会调 {@code Inventory#removeItem}），就会出现
 * “背包先掉、箭袋不掉”，再把射出去的箭捡回来就是可以无限刷箭的漏洞。现在扣箭点只有一个，
 * 且只认箭袋。
 * <p>
 * 键是物品堆叠对象本身（{@link ItemStack} 没有重写 equals/hashCode，天然按引用比较）。
 * 用 {@link WeakHashMap} 是为了不让“只查了一下弹药、没有真的射”的临时副本一直留着。
 */
public final class QuiverAmmo {

    private static final Map<ItemStack, Pending> PENDING =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * @param player        玩家 UUID
     * @param inventorySlot 箭袋在背包里的下标
     * @param quiverSlot    发出去的那一发来自哪个槽
     * @param template      那一发的样板（数量 1），用来在槽位被挪动后重新认人
     */
    public record Pending(UUID player, int inventorySlot, int quiverSlot, ItemStack template) {
    }

    public static void remember(ItemStack ammo, Player player, int inventorySlot, int quiverSlot) {
        PENDING.put(ammo, new Pending(player.getUUID(), inventorySlot, quiverSlot, ammo.copyWithCount(1)));
    }

    /** 取走关联；同一个副本只会被取一次，之后原版逻辑不再经过这里 */
    public static Pending take(ItemStack ammo) {
        return PENDING.remove(ammo);
    }

    /**
     * 接管 {@code ProjectileWeaponItem#useAmmo}：返回值与原版逐条一致，唯一的区别是扣箭只扣箭袋。
     * <p>
     * 客户端也会走这里（预测），但只有服务端真正写组件；客户端只负责算出同样的返回值。
     */
    public static ItemStack consume(ItemStack weapon, ItemStack ammo, LivingEntity shooter,
                                    boolean crit, Pending pending) {
        int use = 0;
        if (!crit && !shooter.hasInfiniteMaterials() && shooter.level() instanceof ServerLevel level) {
            // 与原版一致：无限 II/III 的 ammo_use 在这里结算，触发返还时 use = 0
            use = EnchantmentHelper.processAmmoUse(level, weapon, ammo, 1);
        }
        if (use > ammo.getCount()) {
            return ItemStack.EMPTY;
        }
        if (use == 0) {
            // 没消耗：交给原版当“无形箭”，落地不能捡
            ItemStack single = ammo.copyWithCount(1);
            single.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            return single;
        }

        ItemStack spent = ammo.split(use);
        if (shooter.level() instanceof ServerLevel level) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(pending.player());
            if (player != null) {
                QuiverHelper.deduct(player, pending.inventorySlot(), pending.quiverSlot(), use,
                        pending.template());
            }
        }
        return spent;
    }

    /** 玩家退出 / 换维度时清掉残留 */
    public static void clear() {
        PENDING.clear();
    }

    private QuiverAmmo() {
    }
}
