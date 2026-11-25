// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import ztqjntm.update.config.ModConfig;
import ztqjntm.update.item.quiver.QuiverContents;
import ztqjntm.update.item.quiver.QuiverHelper;
import ztqjntm.update.menu.QuiverMenu;

import java.util.function.Consumer;

/**
 * 箭袋：6 个槽的弹药选择器。
 * <p>
 * 它只决定“下一发射哪一种箭”，不改箭的物理、伤害和命中效果——那些仍然由箭物品自己决定。
 * 右键的三条分支见 {@link #use}；真正和弓弩对接的地方在
 * {@code PlayerMixin#getProjectile} 与 {@code ProjectileWeaponItemMixin#draw}。
 */
public class QuiverItem extends Item {

    public QuiverItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * 右键：
     * <ul>
     *   <li>潜行 → 总是打开界面（另一只手有箭也一样）；</li>
     *   <li>另一只手有箭 → 装填一个槽，装满为止；</li>
     *   <li>其余情况 → 打开界面。</li>
     * </ul>
     * 对着箱子这类可交互方块右键时，原版根本不会走到这里，所以不需要额外判断“是不是方块”。
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!ModConfig.get().quiver.enabled) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof QuiverItem)) {
            return InteractionResult.PASS;
        }

        // 客户端只负责挥臂表现；装填、开界面都由服务端做，避免两边各改一次
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            // 主手拿箭袋就从副手取箭，箭袋在副手就从主手取箭：两只手对称，
            // 不会出现“箭袋换到副手就装不进去”的死角。
            ItemStack source = hand == InteractionHand.MAIN_HAND
                    ? player.getOffhandItem()
                    : player.getMainHandItem();
            if (QuiverHelper.isAllowedAmmo(source)) {
                if (QuiverHelper.insertFrom(player, stack, source)) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
                // 装不进去：箭袋满了，给个低沉提示，不打开界面
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.PLAYERS, 0.8F, 1.0F);
                return InteractionResult.FAIL;
            }
        }

        int inventorySlot = hand == InteractionHand.MAIN_HAND
                ? player.getInventory().getSelectedSlot()
                : Inventory.SLOT_OFFHAND;
        serverPlayer.openMenu(new QuiverMenuProvider(inventorySlot));
        return InteractionResult.SUCCESS;
    }

    /**
     * tooltip：固定 5 行（空槽写“空”，行高不会跳）+ 一行操作提示。
     * <p>
     * 每行只显示“箭名 ×数量”，不把箭自己的伤害 / 效果介绍嵌进来——那样会挤满整个屏幕，
     * 箭本身的 tooltip 已经够了。
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        QuiverContents contents = QuiverHelper.contents(stack);
        for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
            boolean current = i == contents.selected();
            ItemStack ammo = contents.get(i);
            MutableComponent line = Component.literal(current ? "▶ " : "  ");
            if (ammo.isEmpty()) {
                line.append(Component.translatable("tooltip.update-ft.quiver.empty")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                line.append(ammo.getHoverName().copy()
                        .withStyle(current ? ChatFormatting.WHITE : ChatFormatting.GRAY));
                line.append(Component.literal(" ")
                        .append(Component.translatable("tooltip.update-ft.quiver.count", ammo.getCount()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.accept(line);
        }
        tooltip.accept(Component.translatable("tooltip.update-ft.quiver.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * 打开界面时把“箭袋在哪个背包槽”传给客户端。
     * <p>
     * 客户端必须知道这个下标，否则它只能猜“最靠前的箭袋”，打开背包里第二个箭袋时就会
     * 编辑错物品。用 Fabric 的扩展菜单类型带一个 int 过去，比额外发一个 S2C 包可靠。
     */
    private record QuiverMenuProvider(int inventorySlot)
            implements ExtendedScreenHandlerFactory<Integer> {

        @Override
        public Integer getScreenOpeningData(ServerPlayer player) {
            return this.inventorySlot;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.update-ft.quiver");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new QuiverMenu(containerId, inventory, this.inventorySlot);
        }
    }
}
