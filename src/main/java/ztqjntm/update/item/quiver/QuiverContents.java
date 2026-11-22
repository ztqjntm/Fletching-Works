// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.item.quiver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 箭袋的全部状态，作为一个数据组件挂在物品上。
 * <p>
 * 五个槽 + 一个“当前槽”。槽里存的是完整 {@link ItemStack}，所以药箭、光灵箭、其他模组的箭
 * 都能连同组件一起保留；跨槽重复是允许的（6 个槽可以放同一种箭）。
 * <p>
 * 这个记录是不可变的：所有修改都返回新实例，调用方写完必须 {@link #writeTo(ItemStack)} 才会
 * 落到物品上。构造器会做一次规范化（补齐 6 槽、数量钳到 64、无效索引归 0），所以读档、收包、
 * 第三方改坏组件时都不会把非法状态带进游戏。
 *
 * @param slots   固定 6 个；空槽用 {@link ItemStack#EMPTY}
 * @param selected 当前槽，0–5
 * @param version 结构版本，便于以后迁移
 */
public record QuiverContents(List<ItemStack> slots, int selected, int version) {

    /** 槽位数**固定**：不是配置项，配置只能调每槽上限 */
    public static final int SLOT_COUNT = 6;
    /** 当前结构版本 */
    public static final int VERSION = 1;
    /** 单槽的硬上限；配置只能往下调，不能突破这里 */
    public static final int MAX_PER_SLOT = 16;

    public static final QuiverContents EMPTY = new QuiverContents(List.of(), 0, VERSION);

    public static final Codec<QuiverContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(ItemStack.OPTIONAL_CODEC).fieldOf("slots").forGetter(QuiverContents::slots),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(QuiverContents::selected),
            Codec.INT.optionalFieldOf("version", VERSION).forGetter(QuiverContents::version)
    ).apply(instance, QuiverContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuiverContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, QuiverContents::slots,
            ByteBufCodecs.VAR_INT, QuiverContents::selected,
            ByteBufCodecs.VAR_INT, QuiverContents::version,
            QuiverContents::new);

    public QuiverContents {
        List<ItemStack> normalized = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = slots != null && i < slots.size() ? slots.get(i) : null;
            if (stack == null || stack.isEmpty()) {
                normalized.add(ItemStack.EMPTY);
                continue;
            }
            // 复制一份：槽位里的 stack 会和背包/光标共享引用，不能让它被原地 shrink
            ItemStack copy = stack.copy();
            if (copy.getCount() > 64) {
                copy.setCount(64);
            }
            normalized.add(copy);
        }
        slots = List.copyOf(normalized);
        if (selected < 0 || selected >= SLOT_COUNT) {
            selected = 0;
        }
    }

    /** 只读访问；调用方需要往外交时自己 {@link ItemStack#copy()} */
    public ItemStack get(int index) {
        return index >= 0 && index < SLOT_COUNT ? this.slots.get(index) : ItemStack.EMPTY;
    }

    /** 返回替换了某一槽的新实例 */
    public QuiverContents with(int index, ItemStack stack) {
        if (index < 0 || index >= SLOT_COUNT) {
            return this;
        }
        List<ItemStack> copy = new ArrayList<>(this.slots);
        copy.set(index, stack == null ? ItemStack.EMPTY : stack);
        return new QuiverContents(copy, this.selected, this.version);
    }

    public QuiverContents withSelected(int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            return this;
        }
        return new QuiverContents(this.slots, index, this.version);
    }

    /** 当前槽的箭；可能是空 */
    public ItemStack selectedStack() {
        return get(this.selected);
    }

    /** 五个槽的总数，只用于物品模型的状态和“是否全空”判断 */
    public int totalCount() {
        int total = 0;
        for (ItemStack stack : this.slots) {
            total += stack.getCount();
        }
        return total;
    }

    public boolean isEmpty() {
        for (ItemStack stack : this.slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isSlotEmpty(int index) {
        return get(index).isEmpty();
    }

    /**
     * 是否已经装满：每个槽都非空且都达到每槽上限。
     * <p>
     * 用来触发“箭袋装满”进度。上限从配置读，所以把 {@code stackLimit} 调低时，
     * 已经按 16 装满的箭袋也算满（每个槽都 ≥ 上限）。
     */
    public boolean isFull(int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, MAX_PER_SLOT));
        for (ItemStack stack : this.slots) {
            if (stack.isEmpty() || stack.getCount() < effectiveLimit) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从 {@code from} 之后按 {@code direction} 找下一个非空槽；找不到时返回 {@code from}。
     * <p>
     * 循环切换、射空自动跳槽、装填路由都用这一个方法，保证“跳过空槽”只有一种解释。
     */
    public int nextNonEmpty(int from, int direction) {
        int step = direction >= 0 ? 1 : -1;
        for (int i = 1; i <= SLOT_COUNT; i++) {
            int index = Math.floorMod(from + step * i, SLOT_COUNT);
            if (!isSlotEmpty(index)) {
                return index;
            }
        }
        return from;
    }

    /**
     * 当前槽空了、但别的槽还有箭时，把“当前”挪到下一个非空槽。
     * <p>
     * 射空后的跳槽走这里；在界面里手动清空当前槽也走这里，否则箭袋会停在空槽上、
     * 明明有箭却射不出去。全空时保持原槽不动。
     */
    public QuiverContents normalizeSelection() {
        if (!selectedStack().isEmpty() || isEmpty()) {
            return this;
        }
        int next = nextNonEmpty(this.selected, 1);
        return isSlotEmpty(next) ? this : withSelected(next);
    }

    /**
     * 物品模型用的小状态：0 = 空，1 = 只剩 1 支，2 = 只剩 2 支，3 = 正常。
     * <p>
     * 之所以单独存一个组件，是因为 1.21 的物品模型只能按组件值分支；
     * 这样不必把整份内容塞进模型谓词里。
     */
    public int state() {
        int total = totalCount();
        return total <= 0 ? 0 : Math.min(total, 3);
    }

    /**
     * 写回物品：内容组件 + 模型状态组件一起写。
     * <p>
     * 所有修改都必须经过这里，否则物品栏里的贴图会和实际内容对不上。
     */
    public void writeTo(ItemStack stack) {
        stack.set(ModComponents.QUIVER_CONTENTS, this);
        stack.set(ModComponents.QUIVER_STATE, state());
    }
}
