package ztqjntm.update.menu;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 制箭台里放着的东西，按坐标存在每个 {@link ServerLevel} 的存档数据里。
 * <p>
 * 1.21.11 的 {@code minecraft:fletching_table} 是普通 {@code Block}（没有专属方块类，也没有方块实体），
 * 所以走“给方块挂 BlockEntity”这条路要么动不了、要么会毁掉村庄 POI。这里改成按坐标存。
 * <p>
 * <b>每个玩家一份容器</b>：同一张桌子两个人同时打开时，各看各的，谁也拿不走对方的材料，
 * 关界面时也只退还自己放进去的东西。旧版（v1）是整张桌子共用一个容器，存档里没有归属人，
 * 读档时先挂在 {@link #LEGACY_OWNER} 名下，等第一个打开这张桌子的玩家认领。
 * <p>
 * 注意：{@link SimpleContainer} 的 {@code setItem} / {@code removeItem} 不会自动把这里标脏，
 * 所以每个容器都挂了监听调 {@link #setDirty()}——否则 {@code DimensionDataStorage} 只保存
 * {@code isDirty()} 为 true 的条目，重启就丢。
 */
public class FletchingTableData extends SavedData {

    /** 存档文件名，最终是 {@code <world>/data/update_ft_fletching_tables.dat} */
    private static final String FILE_ID = "update_ft_fletching_tables";

    /** 当前数据版本；以后改结构时递增，并在构造函数里加一条迁移分支 */
    private static final int VERSION = 2;

    /** 格子数必须和界面的原料格数量一致 */
    private static final int SLOTS = FletchingTableMenu.INPUT_SIZE;

    /** v1 存档没有归属人，先挂在这个 UUID 下，等玩家打开桌子时认领 */
    private static final UUID LEGACY_OWNER = new UUID(0L, 0L);

    /** v1 的条目：整张桌子一份内容物 */
    private record LegacyEntry(BlockPos pos, List<ItemStack> items) {
        private static final Codec<LegacyEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(LegacyEntry::pos),
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(LegacyEntry::items)
        ).apply(instance, LegacyEntry::new));
    }

    /** v2 的条目：一份内容物属于一个玩家 */
    private record OwnerEntry(UUID owner, List<ItemStack> items) {
        private static final Codec<OwnerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(OwnerEntry::owner),
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(OwnerEntry::items)
        ).apply(instance, OwnerEntry::new));
    }

    /** 一个坐标的桌子，可能有多份（每人一份） */
    private record Entry(BlockPos pos, List<OwnerEntry> owners) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                OwnerEntry.CODEC.listOf().fieldOf("owners").forGetter(Entry::owners)
        ).apply(instance, Entry::new));
    }

    /**
     * 同时接受 v1（{@code items}）和 v2（{@code owners}）两种形状，写出时永远写 v2。
     * <p>
     * 不用 {@code Codec#dispatch}：版本字段先只用来“识别”，真需要复杂迁移时再换 MapCodec 分支。
     */
    public static final Codec<FletchingTableData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("version", 1).forGetter(data -> VERSION),
            Codec.either(LegacyEntry.CODEC.listOf(), Entry.CODEC.listOf())
                    .fieldOf("tables")
                    .forGetter(FletchingTableData::toEitherEntries)
    ).apply(instance, FletchingTableData::new));

    public static final SavedDataType<FletchingTableData> TYPE =
            new SavedDataType<>(FILE_ID, FletchingTableData::new, CODEC, DataFixTypes.LEVEL);

    /** 坐标 → 玩家 → 三格容器 */
    private final Map<BlockPos, Map<UUID, SimpleContainer>> containers = new HashMap<>();

    public FletchingTableData() {
    }

    /** 读档：兼容 v1 的共享容器，并重新挂上标脏监听 */
    private FletchingTableData(int version, Either<List<LegacyEntry>, List<Entry>> tables) {
        if (tables.left().isPresent()) {
            for (LegacyEntry entry : tables.left().get()) {
                load(entry.pos(), List.of(new OwnerEntry(LEGACY_OWNER, entry.items())));
            }
            return;
        }
        for (Entry entry : tables.right().get()) {
            load(entry.pos(), entry.owners());
        }
    }

    private void load(BlockPos pos, List<OwnerEntry> owners) {
        Map<UUID, SimpleContainer> byOwner = new HashMap<>();
        for (OwnerEntry owner : owners) {
            SimpleContainer container = new SimpleContainer(SLOTS);
            List<ItemStack> items = owner.items();
            for (int slot = 0; slot < Math.min(items.size(), SLOTS); slot++) {
                container.setItem(slot, items.get(slot));
            }
            this.watch(container);
            byOwner.put(owner.owner(), container);
        }
        if (!byOwner.isEmpty()) {
            this.containers.put(pos, byOwner);
        }
    }

    /** 每个维度一份 */
    public static FletchingTableData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * 拿到某个玩家在这张桌子上的容器，没有就建一个。
     * <p>
     * 如果这个坐标已经不是制箭台了，先把旧内容物掉在原地再删掉条目，返回一个不进存档的临时容器。
     * 正常流程里这个分支基本走不到（打开界面前已经确认过是制箭台），真正负责收拾
     * 爆炸/活塞/结构破坏残条目的是 {@link #sweep(ServerLevel)}。
     * <p>
     * 第一次打开时会把 {@link #LEGACY_OWNER} 名下的 v1 内容物认领过来。
     */
    public SimpleContainer getOrCreate(ServerLevel level, BlockPos pos, UUID player) {
        Map<UUID, SimpleContainer> byOwner = this.containers.get(pos);

        if (!level.getBlockState(pos).is(Blocks.FLETCHING_TABLE)) {
            if (byOwner != null) {
                this.dropAndRemove(level, pos);
            }
            // 给一个临时容器：调用方（菜单）可能已经建好了，但它不该被写进存档
            return new SimpleContainer(SLOTS);
        }

        if (byOwner == null) {
            byOwner = new HashMap<>();
            this.containers.put(pos, byOwner);
        }

        SimpleContainer own = byOwner.get(player);
        if (own != null) {
            return own;
        }

        own = new SimpleContainer(SLOTS);
        this.watch(own);
        byOwner.put(player, own);
        this.adoptLegacy(byOwner, own);
        this.setDirty();
        return own;
    }

    /** v1 的内容物没有主人，第一个打开桌子的玩家接手 */
    private void adoptLegacy(Map<UUID, SimpleContainer> byOwner, SimpleContainer target) {
        SimpleContainer legacy = byOwner.remove(LEGACY_OWNER);
        if (legacy == null) {
            return;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = legacy.getItem(slot);
            if (!stack.isEmpty()) {
                target.setItem(slot, stack);
            }
        }
        legacy.clearContent();
    }

    /**
     * 扫描一遍残条目，给爆炸/活塞/结构破坏兜底。
     * <p>
     * 这些破坏方式不会触发 {@code PlayerBlockBreakEvents}，桌子没了但存档条目还在，
     * 内容物就永远取不出来。这里只检查**已经加载**的区块（不为了清理去加载区块），
     * 桌子不在就把内容物掉在原地并删条目；区块没加载的留到下次加载后再查。
     */
    public void sweep(ServerLevel level) {
        if (this.containers.isEmpty()) {
            return;
        }
        List<BlockPos> stale = new ArrayList<>();
        for (BlockPos pos : this.containers.keySet()) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!level.getBlockState(pos).is(Blocks.FLETCHING_TABLE)) {
                stale.add(pos);
            }
        }
        for (BlockPos pos : stale) {
            this.dropAndRemove(level, pos);
        }
    }

    /** 这个玩家的内容物已经还给他了，只删条目、不再往地上掉东西（正常关界面走这里） */
    public void discard(BlockPos pos, UUID player) {
        Map<UUID, SimpleContainer> byOwner = this.containers.get(pos);
        if (byOwner == null) {
            return;
        }
        if (byOwner.remove(player) != null) {
            this.setDirty();
        }
        if (byOwner.isEmpty()) {
            this.containers.remove(pos);
        }
    }

    /** 把某个坐标所有玩家的内容物掉在原地并删掉条目（拆桌、{@link #sweep} 发现的残条目都走这里） */
    public void dropAndRemove(ServerLevel level, BlockPos pos) {
        Map<UUID, SimpleContainer> byOwner = this.containers.remove(pos);
        if (byOwner == null) {
            return;
        }
        for (SimpleContainer container : byOwner.values()) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
            container.clearContent();
        }
        this.setDirty();
    }

    /**
     * 把当前维度所有残留条目倒给执行者并清空，给“卸载模组前忘了清桌子”留一条后路。
     *
     * @return 倒出的物品组数（用于命令反馈）
     */
    public int dumpTo(Player player) {
        int stacks = 0;
        for (Map<UUID, SimpleContainer> byOwner : this.containers.values()) {
            for (SimpleContainer container : byOwner.values()) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (!stack.isEmpty()) {
                        player.drop(stack.copy(), false);
                        container.setItem(slot, ItemStack.EMPTY);
                        stacks++;
                    }
                }
            }
        }
        if (stacks > 0) {
            this.containers.clear();
            this.setDirty();
        }
        return stacks;
    }

    /** 容器一有变化就标脏，否则重启后内容会丢 */
    private void watch(SimpleContainer container) {
        container.addListener(changed -> this.setDirty());
    }

    private Either<List<LegacyEntry>, List<Entry>> toEitherEntries() {
        return Either.right(this.toEntries());
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>();
        this.containers.forEach((pos, byOwner) -> {
            List<OwnerEntry> owners = new ArrayList<>();
            byOwner.forEach((owner, container) -> {
                List<ItemStack> items = new ArrayList<>(SLOTS);
                boolean empty = true;
                for (int slot = 0; slot < SLOTS; slot++) {
                    ItemStack stack = container.getItem(slot);
                    items.add(stack);
                    empty &= stack.isEmpty();
                }
                // 空容器不必写进存档；下次打开会重新建一个
                if (!empty) {
                    owners.add(new OwnerEntry(owner, items));
                }
            });
            if (!owners.isEmpty()) {
                entries.add(new Entry(pos, owners));
            }
        });
        return entries;
    }
}
