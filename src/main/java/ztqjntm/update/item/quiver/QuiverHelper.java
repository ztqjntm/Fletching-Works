// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.item.quiver;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import ztqjntm.update.advancement.ModAdvancements;
import ztqjntm.update.config.ModConfig;
import ztqjntm.update.item.QuiverItem;
import ztqjntm.update.tag.ModTags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 箭袋的读取 / 装填 / 扣除，全部集中在这里。
 * <p>
 * 客户端和服务端都会调用这些方法：客户端拿它画 HUD、做预测，服务端拿它做真正的判定。
 * 所有写操作都只改组件，不直接改背包结构；写完之后由调用方决定是否 {@code setChanged()}。
 */
public final class QuiverHelper {

    /** 生效箭袋的候选范围：快捷栏 0–8 + 主背包 9–35；副手槽不在这段里，单独判一次 */
    private static final int EFFECTIVE_RANGE = Inventory.INVENTORY_SIZE;

    /** {@code lockOnDraw} 用：玩家 UUID → 拉弓那一刻锁定的槽位（-1 表示当时没有箭袋） */
    private static final Map<UUID, Integer> LOCKED_SLOT = new HashMap<>();

    public static boolean isQuiver(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof QuiverItem;
    }

    /** 读内容；不是箭袋、或组件被第三方清掉时按空箭袋处理，不抛异常 */
    public static QuiverContents contents(ItemStack stack) {
        if (!isQuiver(stack)) {
            return QuiverContents.EMPTY;
        }
        QuiverContents contents = stack.get(ModComponents.QUIVER_CONTENTS);
        return contents == null ? QuiverContents.EMPTY : contents;
    }

    /**
     * 生效箭袋在背包里的下标；没有则 -1。
     * <p>
     * 优先级：<b>主手 → 副手 → 快捷栏 → 主背包</b>。手里拿着的箭袋排最前，因为“拿着”是最明确的
     * 意图，也和原版 {@code getProjectile} 先看手持弹药的结构一致；副手拿的是箭时，原版会先用手持
     * 的箭，轮不到箭袋（{@code getHeldProjectile} 在 mixin 里先跑）。
     * <p>
     * 副手槽 {@link Inventory#SLOT_OFFHAND} 不在 0–35 里，必须单独判一次，否则“副手箭袋”永远
     * 不会成为生效箭袋，HUD、按键、弹药查询都会当它不存在。
     */
    public static int findEffectiveSlot(Player player) {
        Inventory inventory = player.getInventory();
        int mainHand = inventory.getSelectedSlot();
        if (isQuiver(inventory.getItem(mainHand))) {
            return mainHand;
        }
        if (isQuiver(inventory.getItem(Inventory.SLOT_OFFHAND))) {
            return Inventory.SLOT_OFFHAND;
        }
        for (int i = 0; i < EFFECTIVE_RANGE; i++) {
            if (isQuiver(inventory.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack effectiveQuiver(Player player) {
        int slot = findEffectiveSlot(player);
        return slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
    }

    /**
     * 能放进箭袋的东西：{@code #minecraft:arrows} 里的任何箭，减去数据包黑名单。
     * <p>
     * 用原版标签而不是本模组的 {@code #update-ft:arrows}，是为了“能装进去的 = 能射出去的”：
     * 原版箭、药箭、光灵箭、其他模组注册进该标签的箭都能用。
     */
    public static boolean isAllowedAmmo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return ModTags.isIn(stack, ItemTags.ARROWS) && !ModTags.isIn(stack, ModTags.QUIVER_BLACKLIST);
    }

    /** 每槽上限：设计上是 16，配置只能往下调 */
    public static int slotLimit() {
        return ModConfig.get().quiver.stackLimit;
    }

    /**
     * 从副手往箭袋里装一次：先补同类未满的槽，再找空槽，**一次只处理一个槽**。
     * <p>
     * 一次只处理一个槽是有意的：右键是高频动作，装满一整叠 64 支会一口气吃掉 4 个槽，
     * 玩家反而没法控制“哪种箭放第几槽”。想一次铺开就用界面里的 Shift + 点击。
     *
     * @return 是否真的装进去了
     */
    public static boolean insertFrom(Player player, ItemStack quiver, ItemStack source) {
        if (!isAllowedAmmo(source) || !isQuiver(quiver)) {
            return false;
        }
        int limit = slotLimit();
        QuiverContents contents = contents(quiver);
        int remaining = source.getCount();

        for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
            ItemStack inSlot = contents.get(i);
            if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(inSlot, source)) {
                continue;
            }
            int space = limit - inSlot.getCount();
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, remaining);
            contents = contents.with(i, inSlot.copyWithCount(inSlot.getCount() + moved));
            remaining -= moved;
            break;
        }

        if (remaining == source.getCount()) {
            for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
                if (!contents.isSlotEmpty(i)) {
                    continue;
                }
                int moved = Math.min(limit, remaining);
                contents = contents.with(i, source.copyWithCount(moved));
                remaining -= moved;
                break;
            }
        }

        int moved = source.getCount() - remaining;
        if (moved <= 0) {
            return false;
        }
        contents.writeTo(quiver);
        source.shrink(moved);
        player.getInventory().setChanged();
        if (player instanceof ServerPlayer serverPlayer) {
            checkFull(serverPlayer, contents);
        }
        return true;
    }

    /**
     * 给武器挑一发弹药：返回**副本**，并记下它来自哪个槽。
     * <p>
     * 武器自己的弹药谓词优先：箭袋当前槽的箭不在武器接受范围内时返回空，让原版继续往下找
     * （而不是硬塞给它）。返回空也意味着“箭袋这里没有可用弹药”，调用方应当放行原版结果。
     * <p>
     * 当前槽意外为空时会退到下一个非空槽：只要箭袋里还有箭，就不会让原版回退到背包。
     * <p>
     * {@code lockOnDraw} 打开时，服务端会用“拉弓那一刻记下的槽”而不是当前槽——这就是
     * “拉弓即承诺”：拉弓期间滚轮改的是下一发，不会改这一发。
     */
    public static ItemStack pickForWeapon(Player player, ProjectileWeaponItem weapon) {
        int inventorySlot = findEffectiveSlot(player);
        if (inventorySlot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack quiver = player.getInventory().getItem(inventorySlot);
        QuiverContents contents = contents(quiver);
        int index = lockedIndex(player, contents);
        if (contents.isSlotEmpty(index)) {
            int next = contents.nextNonEmpty(index, 1);
            if (contents.isSlotEmpty(next)) {
                return ItemStack.EMPTY;
            }
            index = next;
        }
        ItemStack ammo = contents.get(index);
        if (!weapon.getAllSupportedProjectiles().test(ammo)) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = ammo.copy();
        QuiverAmmo.remember(copy, player, inventorySlot, index);
        return copy;
    }

    private static int lockedIndex(Player player, QuiverContents contents) {
        if (!player.level().isClientSide() && ModConfig.get().quiver.lockOnDraw) {
            Integer locked = LOCKED_SLOT.get(player.getUUID());
            if (locked != null && locked >= 0 && locked < QuiverContents.SLOT_COUNT
                    && !contents.isSlotEmpty(locked)) {
                return locked;
            }
        }
        return contents.selected();
    }

    /** 每个服务端 tick 调一次：开始拉弓的那一刻记下当前槽，松开后清掉 */
    public static void tickLock(ServerPlayer player) {
        if (!ModConfig.get().quiver.enabled || !ModConfig.get().quiver.lockOnDraw) {
            return;
        }
        boolean drawing = player.isUsingItem()
                && player.getUseItem().getItem() instanceof ProjectileWeaponItem;
        if (!drawing) {
            LOCKED_SLOT.remove(player.getUUID());
            return;
        }
        if (LOCKED_SLOT.containsKey(player.getUUID())) {
            return;
        }
        int slot = findEffectiveSlot(player);
        LOCKED_SLOT.put(player.getUUID(), slot < 0 ? -1 : contents(player.getInventory().getItem(slot)).selected());
    }

    /** 玩家退出时清掉锁定记录 */
    public static void clearLock(UUID playerId) {
        LOCKED_SLOT.remove(playerId);
    }

    /**
     * 从箭袋扣箭：**只扣箭袋，绝不碰背包**。
     * <p>
     * 先核对发出去的那一发来自哪个槽（{@code template} 是它的单个样板）；槽位被挪动过就按
     * “同物品同组件”在箭袋里重新找。找不到就什么都不扣——宁可这一发不扣，也不能回退去扣
     * 背包里的同种箭：那会让“背包先掉、箭袋不掉”，配合地上捡回就是可以无限刷箭的漏洞。
     * <p>
     * 扣空之后自动跳到下一个非空槽（可能还是同一种箭）。只在服务端调用。
     */
    public static void deduct(Player player, int inventorySlot, int quiverSlot, int amount, ItemStack template) {
        if (amount <= 0) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack quiver = inventory.getItem(inventorySlot);
        if (!isQuiver(quiver)) {
            return;
        }
        QuiverContents contents = contents(quiver);
        int index = quiverSlot;
        if (!ItemStack.isSameItemSameComponents(contents.get(index), template)) {
            index = -1;
            for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
                if (ItemStack.isSameItemSameComponents(contents.get(i), template)) {
                    index = i;
                    break;
                }
            }
        }
        if (index < 0) {
            return;
        }
        ItemStack inSlot = contents.get(index);
        int left = Math.max(0, inSlot.getCount() - amount);
        QuiverContents updated = contents.with(index,
                left == 0 ? ItemStack.EMPTY : inSlot.copyWithCount(left)).normalizeSelection();
        updated.writeTo(quiver);
        inventory.setChanged();
    }

    /** 箭袋装满时触发一次进度；服务端调用，重复触发由进度系统自己忽略 */
    public static void checkFull(ServerPlayer player, QuiverContents contents) {
        if (contents.isFull(slotLimit())) {
            ModAdvancements.QUIVER_FULL.trigger(player);
        }
    }

    private QuiverHelper() {
    }
}
