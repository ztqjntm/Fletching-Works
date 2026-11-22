// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.menu;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ztqjntm.update.item.quiver.QuiverContents;
import ztqjntm.update.item.quiver.QuiverHelper;

/**
 * 箭袋界面：5 个弹药槽 + 玩家背包。
 * <p>
     * 槽位坐标按 {@code textures/gui/quiver.png} 排版：贴图里 6 个 18×18 槽框在
     * {@code x = 33, 51, 69, 87, 105, 123 / y = 33}，物品区再各 +1，所以这里是 34 起步、步进 18。
     * 换贴图时两边要一起改。
 */
public class QuiverMenu extends AbstractContainerMenu {

    /** 弹药槽数量，和组件结构绑定 */
    public static final int SLOT_COUNT = QuiverContents.SLOT_COUNT;
    /** 玩家背包在 slots 列表里的起点 */
    private static final int INV_START = SLOT_COUNT;

    private final Inventory inventory;
    private final int inventorySlot;

    /**
     * @param inventorySlot 箭袋在玩家背包里的下标（0–35 主背包/快捷栏，40 副手）
     */
    public QuiverMenu(int containerId, Inventory inventory, int inventorySlot) {
        super(ModMenuTypes.QUIVER, containerId);
        this.inventory = inventory;
        this.inventorySlot = inventorySlot;

        QuiverContainer container = new QuiverContainer(inventory, inventorySlot,
                inventory.player.level().isClientSide());
        for (int i = 0; i < SLOT_COUNT; i++) {
            // 贴图里 6 个 18×18 槽框在 x = 33, 51, … / y = 33，物品区各 +1
            this.addSlot(new AmmoSlot(container, i, 34 + i * 18, 34));
        }
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    public int inventorySlot() {
        return this.inventorySlot;
    }

    /**
     * 当前槽（界面高亮用）。
     * <p>
     * 读的是物品组件里的 {@code selected}：客户端那份由切换包 / 同步包更新，
     * 而 6 个槽的内容走菜单自己的同步，两者互不干扰。
     */
    public int selected() {
        return QuiverHelper.contents(this.inventory.getItem(this.inventorySlot)).selected();
    }

    /**
     * 打开期间箭袋被丢出、被整理模组移走、或被换成别的东西时立刻关闭，
     * 避免继续往一个已经不在背包里的物品写内容。
     */
    @Override
    public boolean stillValid(Player player) {
        return QuiverHelper.isQuiver(this.inventory.getItem(this.inventorySlot));
    }

    /**
     * 每次点击后按“箭袋里的总数是变多还是变少”播一次收纳音效。
     * <p>
     * 不在 {@code Slot} 上逐个播：Shift 点一整叠 64 支会铺开 4 个槽，那样会连响 4 次。
     * 这里整次点击只响一次，方向和玩家的动作一致。
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int before = totalAmmo();
        super.clicked(slotId, button, clickType, player);
        int after = totalAmmo();
        if (after > before) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.6F, 1.0F);
        } else if (after < before) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    private int totalAmmo() {
        return QuiverHelper.contents(this.inventory.getItem(this.inventorySlot)).totalCount();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();

        if (index < SLOT_COUNT) {
            // 箭袋 → 背包
            if (!this.moveItemStackTo(stack, INV_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 背包 → 箭袋：只收箭；先补同类未满槽，再找空槽。
            // 一整叠 64 支会按每槽 16 铺开到多个槽——这是“快速装满”的主路径。
            if (!QuiverHelper.isAllowedAmmo(stack)) {
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return clicked;
    }

    /** 只接受 {@code #minecraft:arrows}（减黑名单）；上限由容器的 getMaxStackSize 决定 */
    private static final class AmmoSlot extends Slot {
        private AmmoSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return QuiverHelper.isAllowedAmmo(stack);
        }
    }
}
