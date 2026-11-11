package ztqjntm.update.menu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 制箭台里放着的东西，按坐标存在每个 {@link ServerLevel} 的存档数据里。
 * <p>
 * 1.21.11 的 {@code minecraft:fletching_table} 是普通 {@code Block}（没有专属方块类，也没有方块实体），
 * 所以走“给方块挂 BlockEntity”这条路要么动不了、要么会毁掉村庄 POI。这里改成按坐标存：
 * 打开界面时 {@link #getOrCreate} 拿到同一个容器，关界面不清空，两个玩家自然共享，重启也还在。
 * <p>
 * 注意：{@link SimpleContainer} 的 {@code setItem} / {@code removeItem} 不会自动把这里标脏，
 * 所以每个容器都挂了监听调 {@link #setDirty()}——否则 {@code DimensionDataStorage} 只保存
 * {@code isDirty()} 为 true 的条目，重启就丢。
 */
public class FletchingTableData extends SavedData {

    /** 存档文件名，最终是 {@code <world>/data/update_ft_fletching_tables.dat} */
    private static final String FILE_ID = "update_ft_fletching_tables";

    /** 格子数必须和界面的原料格数量一致 */
    private static final int SLOTS = FletchingTableMenu.INPUT_SIZE;

    /** 一条记录：某个坐标的桌子 + 三个格子里的东西（空栈也会写，保证下标对齐） */
    private record Entry(BlockPos pos, List<ItemStack> items) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(Entry::items)
        ).apply(instance, Entry::new));
    }

    public static final Codec<FletchingTableData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("tables").forGetter(FletchingTableData::toEntries)
    ).apply(instance, FletchingTableData::new));

    public static final SavedDataType<FletchingTableData> TYPE =
            new SavedDataType<>(FILE_ID, FletchingTableData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<BlockPos, SimpleContainer> containers = new HashMap<>();

    /**
     * 记录“这张桌子现在有几个人开着”。
     * <p>
     * 关界面时要把原料退回玩家（桌上不留东西，免得玩家忘了拿还以为被吞），但两个人同时开着同一张桌子时，
     * 先关的那个人不该把另一位正在用的材料收走——所以等最后一个玩家关闭时再结算。
     * {@link #startOpen} / {@link #stopOpen} 由菜单在构造和 {@code removed()} 里调用。
     * <p>
     * 用 {@link Set} 而不是计数器：同一个人重复打开同一张桌子不会重复计数；玩家掉线时
     * {@code removed()} 有可能没走到，{@link #isViewed()} 会把已经移除的实体剔掉，
     * 避免“计数永远大于 0、桌子永远不结算”的泄漏。
     */
    public static final class SharedContainer extends SimpleContainer {
        private final Set<ContainerUser> viewers = new HashSet<>();

        private SharedContainer() {
            super(SLOTS);
        }

        @Override
        public void startOpen(ContainerUser user) {
            if (user != null) {
                this.viewers.add(user);
            }
        }

        @Override
        public void stopOpen(ContainerUser user) {
            if (user != null) {
                this.viewers.remove(user);
            }
        }

        /**
         * 还有别人开着这张桌子；顺手剔掉已经失效的查看者：
         * 已经不在世界里的实体，以及“当前菜单已经不是这张桌子”的玩家
         * （例如菜单构造了但 {@code openMenu} 没成功，或者关闭时漏掉了 {@code stopOpen}）。
         */
        public boolean isViewed() {
            this.viewers.removeIf(user -> {
                if (!(user instanceof Entity entity) || entity.isRemoved()) {
                    return true;
                }
                if (user instanceof Player player) {
                    return !(player.containerMenu instanceof FletchingTableMenu menu) || !menu.uses(this);
                }
                return false;
            });
            return !this.viewers.isEmpty();
        }
    }

    public FletchingTableData() {
    }

    /** 读档：把每条记录还原成容器，并重新挂上标脏监听 */
    private FletchingTableData(List<Entry> entries) {
        for (Entry entry : entries) {
            SimpleContainer container = new SharedContainer();
            List<ItemStack> items = entry.items();
            for (int slot = 0; slot < Math.min(items.size(), SLOTS); slot++) {
                container.setItem(slot, items.get(slot));
            }
            this.watch(container);
            this.containers.put(entry.pos(), container);
        }
    }

    /** 每个维度一份 */
    public static FletchingTableData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * 拿到某个坐标的容器，没有就建一个。
     * <p>
     * 如果这个坐标已经不是制箭台了，先把旧内容物掉在原地再删掉条目，返回一个不进存档的临时容器。
     * 正常流程里这个分支基本走不到（打开界面前已经确认过是制箭台），真正负责收拾
     * 爆炸/活塞/结构破坏残条目的是 {@link #sweep(ServerLevel)}。
     */
    public SimpleContainer getOrCreate(ServerLevel level, BlockPos pos) {
        SimpleContainer existing = this.containers.get(pos);

        if (!level.getBlockState(pos).is(Blocks.FLETCHING_TABLE)) {
            if (existing != null) {
                this.dropAndRemove(level, pos);
            }
            // 给一个临时容器：调用方（菜单）可能已经建好了，但它不该被写进存档
            return new SharedContainer();
        }

        if (existing != null) {
            return existing;
        }

        SharedContainer created = new SharedContainer();
        this.watch(created);
        this.containers.put(pos, created);
        this.setDirty();
        return created;
    }

    /**
     * 扫描一遍残条目，给爆炸/活塞/结构破坏兜底。
     * <p>
     * 这些破坏方式不会触发 {@code PlayerBlockBreakEvents}，桌子没了但存档条目还在，
     * 内容物就永远取不出来。这里只检查**已经加载**的区块（不为了清理去加载区块），
     * 桌子不在就把内容物掉在原地并删条目；区块没加载的留到下次加载后再查。
     * <p>
     * 还有人开着这张桌子时先跳过：关界面时 {@code removed()} 会正常结算，别把东西提前倒在地上。
     */
    public void sweep(ServerLevel level) {
        if (this.containers.isEmpty()) {
            return;
        }
        List<BlockPos> stale = new ArrayList<>();
        this.containers.forEach((pos, container) -> {
            if (container instanceof SharedContainer shared && shared.isViewed()) {
                return;
            }
            if (!level.isLoaded(pos)) {
                return;
            }
            if (!level.getBlockState(pos).is(Blocks.FLETCHING_TABLE)) {
                stale.add(pos);
            }
        });
        for (BlockPos pos : stale) {
            this.dropAndRemove(level, pos);
        }
    }

    /** 内容已经还给玩家了，只删条目、不再往地上掉东西（正常关界面走这里） */
    public void discard(BlockPos pos) {
        if (this.containers.remove(pos) != null) {
            this.setDirty();
        }
    }

    /** 把某个坐标的内容物掉在原地并删掉条目（玩家拆桌、{@link #sweep(ServerLevel)} 发现的残条目都走这里） */
    public void dropAndRemove(ServerLevel level, BlockPos pos) {
        SimpleContainer container = this.containers.remove(pos);
        if (container == null) {
            return;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
        container.clearContent();
        this.setDirty();
    }

    /** 容器一有变化就标脏，否则重启后内容会丢 */
    private void watch(SimpleContainer container) {
        container.addListener(changed -> this.setDirty());
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>();
        this.containers.forEach((pos, container) -> {
            List<ItemStack> items = new ArrayList<>(SLOTS);
            boolean empty = true;
            for (int slot = 0; slot < SLOTS; slot++) {
                ItemStack stack = container.getItem(slot);
                items.add(stack);
                empty &= stack.isEmpty();
            }
            // 空桌子不必写进存档；下次打开会重新建一个空容器
            if (!empty) {
                entries.add(new Entry(pos, items));
            }
        });
        return entries;
    }
}
