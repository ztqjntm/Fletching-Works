// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ztqjntm.update.item.quiver.QuiverContents;
import ztqjntm.update.item.quiver.QuiverHelper;
import ztqjntm.update.network.ModNetworking;

/**
 * 把箭袋物品的组件包装成 {@link Container}，给 {@link QuiverMenu} 的 6 个槽用。
 * <p>
 * 服务端：每次写入都落到物品组件上，并把整份内容同步给打开界面的玩家——界面开着的时候
 * 玩家的背包菜单不再广播，不主动同步的话“当前槽”高亮会停在旧位置。
 * <p>
 * 客户端：只维护一份本地数组，不往物品组件里写。客户端那份组件本来就可能比服务端旧，
 * 拿它做“写入再读回”只会把旧值又写回去；6 个槽的内容由菜单同步包负责，高亮由
 * {@link QuiverMenu#selected()} 负责。
 * <p>
 * 这里没有“关界面退还”逻辑：内容本来就在物品组件里，跟着物品走。这一点和制箭台
 * （按坐标 + 玩家存 {@code SavedData}）完全不同，不要照抄那边的清空逻辑。
 */
public class QuiverContainer implements Container {

    private final Inventory inventory;
    private final int inventorySlot;
    private final boolean clientSide;
    /** 只在客户端使用：菜单同步下来的 6 个槽 */
    private final ItemStack[] clientSlots = new ItemStack[QuiverContents.SLOT_COUNT];

    public QuiverContainer(Inventory inventory, int inventorySlot, boolean clientSide) {
        this.inventory = inventory;
        this.inventorySlot = inventorySlot;
        this.clientSide = clientSide;
        if (clientSide) {
            QuiverContents contents = QuiverHelper.contents(this.inventory.getItem(inventorySlot));
            for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
                this.clientSlots[i] = contents.get(i).copy();
            }
        }
    }

    public int inventorySlot() {
        return this.inventorySlot;
    }

    /** 当前那个槽里的箭袋；不是箭袋时返回空堆叠 */
    private ItemStack quiver() {
        ItemStack stack = this.inventory.getItem(this.inventorySlot);
        return QuiverHelper.isQuiver(stack) ? stack : ItemStack.EMPTY;
    }

    @Override
    public int getContainerSize() {
        return QuiverContents.SLOT_COUNT;
    }

    /** 每槽上限由配置决定，{@code Slot} 与 Shift 路由都会读这里 */
    @Override
    public int getMaxStackSize() {
        return QuiverHelper.slotLimit();
    }

    @Override
    public boolean isEmpty() {
        if (this.clientSide) {
            for (ItemStack stack : this.clientSlots) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        return QuiverHelper.contents(quiver()).isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= QuiverContents.SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        if (this.clientSide) {
            return this.clientSlots[index];
        }
        return QuiverHelper.contents(quiver()).get(index);
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        if (index < 0 || index >= QuiverContents.SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (this.clientSide) {
            ItemStack inSlot = this.clientSlots[index];
            if (inSlot.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int taken = Math.min(amount, inSlot.getCount());
            ItemStack removed = inSlot.copyWithCount(taken);
            this.clientSlots[index] = taken >= inSlot.getCount()
                    ? ItemStack.EMPTY : inSlot.copyWithCount(inSlot.getCount() - taken);
            return removed;
        }

        ItemStack current = quiver();
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        QuiverContents contents = QuiverHelper.contents(current);
        ItemStack inSlot = contents.get(index);
        if (inSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, inSlot.getCount());
        ItemStack removed = inSlot.copyWithCount(taken);
        ItemStack left = taken >= inSlot.getCount() ? ItemStack.EMPTY : inSlot.copyWithCount(inSlot.getCount() - taken);
        this.write(current, contents.with(index, left).normalizeSelection());
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return this.removeItem(index, Integer.MAX_VALUE);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= QuiverContents.SLOT_COUNT) {
            return;
        }
        ItemStack stored = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!stored.isEmpty() && stored.getCount() > QuiverHelper.slotLimit()) {
            stored = stored.copyWithCount(QuiverHelper.slotLimit());
        }

        if (this.clientSide) {
            this.clientSlots[index] = stored;
            return;
        }

        ItemStack current = quiver();
        if (current.isEmpty()) {
            return;
        }
        this.write(current, QuiverHelper.contents(current).with(index, stored).normalizeSelection());
    }

    @Override
    public void setChanged() {
        this.inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return QuiverHelper.isQuiver(this.inventory.getItem(this.inventorySlot));
    }

    @Override
    public void clearContent() {
        if (this.clientSide) {
            for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
                this.clientSlots[i] = ItemStack.EMPTY;
            }
            return;
        }
        ItemStack current = quiver();
        if (current.isEmpty()) {
            return;
        }
        this.write(current, QuiverContents.EMPTY);
    }

    /** 服务端唯一写入口：写组件 + 标脏 + 同步给正在看界面的玩家 */
    private void write(ItemStack quiver, QuiverContents contents) {
        contents.writeTo(quiver);
        this.inventory.setChanged();
        if (this.inventory.player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendQuiverSync(serverPlayer, this.inventorySlot, contents);
            // 装满直接给成就：这个状态只有“写组件”这一条路能达到
            QuiverHelper.checkFull(serverPlayer, contents);
        }
    }
}
