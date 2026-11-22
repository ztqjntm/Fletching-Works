// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.item.quiver;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ztqjntm.update.GameTestBootstrap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件结构：不管数据包、旧存档还是第三方模组写进来什么，读出来都必须是“恰好 6 槽、
 * 当前槽在 0–5、每槽数量合法”的状态，否则界面和 HUD 会一起越界。
 */
class QuiverContentsTest {

    private static RegistryOps<JsonElement> ops() {
        return RegistryOps.create(JsonOps.INSTANCE,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    private static QuiverContents decode(String json) {
        var parsed = QuiverContents.CODEC.parse(ops(), JsonParser.parseString(json));
        assertTrue(parsed.result().isPresent(), () -> "解析失败: " + parsed.error().orElse(null));
        return parsed.result().get();
    }

    private static ItemStack arrows(int count) {
        return new ItemStack(Items.ARROW, count);
    }

    @BeforeAll
    static void bootstrap() {
        GameTestBootstrap.bootstrap();
    }

    @Test
    void padsMissingSlotsToSix() {
        QuiverContents contents = decode("""
                {"slots":[{"id":"minecraft:arrow","count":5}]}
                """);
        assertEquals(QuiverContents.SLOT_COUNT, contents.slots().size());
        assertEquals(5, contents.get(0).getCount());
        assertTrue(contents.get(QuiverContents.SLOT_COUNT - 1).isEmpty());
    }

    @Test
    void clampsInvalidSelection() {
        assertEquals(0, decode("""
                {"slots":[{"id":"minecraft:arrow","count":5}],"selected":9}
                """).selected());
        assertEquals(0, decode("""
                {"slots":[{"id":"minecraft:arrow","count":5}],"selected":-3}
                """).selected());
    }

    @Test
    void acceptsLastSlotAsSelection() {
        QuiverContents contents = decode("""
                {"slots":[{},{},{},{},{},{"id":"minecraft:arrow","count":2}],"selected":5}
                """);
        assertEquals(5, contents.selected());
        assertEquals(2, contents.selectedStack().getCount());
    }

    @Test
    void isFullRequiresEverySlotAtLimit() {
        QuiverContents full = new QuiverContents(
                List.of(arrows(16), arrows(16), arrows(16), arrows(16), arrows(16), arrows(16)), 0, 1);
        assertTrue(full.isFull(16));
        // 上限调低时，已经装到 16 的箭袋仍然算满
        assertTrue(full.isFull(8));

        QuiverContents oneShort = full.with(3, arrows(15));
        assertFalse(oneShort.isFull(16));
        assertTrue(oneShort.isFull(15));

        QuiverContents emptySlot = full.with(5, ItemStack.EMPTY);
        assertFalse(emptySlot.isFull(16));
    }

    @Test
    void roundTripsCountsAndSelection() {
        QuiverContents original = new QuiverContents(
                List.of(arrows(7), ItemStack.EMPTY, arrows(16), ItemStack.EMPTY, ItemStack.EMPTY), 2, 1);
        JsonElement encoded = QuiverContents.CODEC.encodeStart(ops(), original).result().orElseThrow();
        QuiverContents decoded = QuiverContents.CODEC.parse(ops(), encoded).result().orElseThrow();

        assertEquals(2, decoded.selected());
        assertEquals(7, decoded.get(0).getCount());
        assertEquals(16, decoded.get(2).getCount());
        assertTrue(decoded.get(1).isEmpty());
    }

    @Test
    void nextNonEmptySkipsEmptySlotsAndWraps() {
        QuiverContents contents = new QuiverContents(
                List.of(arrows(1), ItemStack.EMPTY, ItemStack.EMPTY, arrows(2), ItemStack.EMPTY), 0, 1);

        assertEquals(3, contents.nextNonEmpty(0, 1));
        // 反向从 0 走：4 空 → 3 有箭
        assertEquals(3, contents.nextNonEmpty(0, -1));
        // 从 3 往后只剩空的，绕回 0
        assertEquals(0, contents.nextNonEmpty(3, 1));
    }

    @Test
    void nextNonEmptyStaysPutWhenAllEmpty() {
        assertEquals(2, QuiverContents.EMPTY.nextNonEmpty(2, 1));
        assertEquals(2, QuiverContents.EMPTY.nextNonEmpty(2, -1));
    }

    @Test
    void modelStateReflectsTotalCount() {
        assertEquals(0, QuiverContents.EMPTY.state());
        assertEquals(1, new QuiverContents(List.of(arrows(1)), 0, 1).state());
        assertEquals(2, new QuiverContents(List.of(arrows(2)), 0, 1).state());
        // 3 支以上都是“正常”外观
        assertEquals(3, new QuiverContents(List.of(arrows(16), arrows(16)), 0, 1).state());
    }

    @Test
    void normalizeSelectionMovesOffEmptyCurrentSlot() {
        QuiverContents contents = new QuiverContents(
                List.of(ItemStack.EMPTY, arrows(3), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY), 0, 1);
        assertEquals(1, contents.normalizeSelection().selected());
        // 全空时停在原槽，不要跳到一个随机的槽
        assertEquals(0, QuiverContents.EMPTY.normalizeSelection().selected());
    }

    @Test
    void withDoesNotMutateOriginal() {
        QuiverContents original = new QuiverContents(List.of(arrows(4)), 0, 1);
        QuiverContents changed = original.with(1, arrows(9));

        assertTrue(original.get(1).isEmpty());
        assertEquals(9, changed.get(1).getCount());
        assertEquals(4, original.get(0).getCount());
    }
}
