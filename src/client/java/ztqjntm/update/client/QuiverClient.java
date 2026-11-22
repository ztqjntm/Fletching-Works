// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import ztqjntm.update.UpdateFt;
import ztqjntm.update.config.ModConfig;
import ztqjntm.update.item.quiver.QuiverContents;
import ztqjntm.update.item.quiver.QuiverHelper;
import ztqjntm.update.network.QuiverSwitchPayload;
import ztqjntm.update.network.QuiverSyncPayload;

/**
 * 客户端一侧的箭袋操作：V 循环 / Shift 反向 / 按住 V 数字直选 / 拉弓时滚轮切换。
 * <p>
 * 所有切换都只是“发请求”，真正改组件的是服务端；客户端只在观察到组件变化后给音效和浮字，
 * 这样多人游戏里不会出现“本地切了、服务端没切”的错觉。
 */
public final class QuiverClient {

    /** 短按判定：按下到松开不超过这个时间才算“点一下循环” */
    private static final long TAP_THRESHOLD_MS = 200L;

    private static KeyMapping switchKey;

    private static boolean holdingKey;
    private static long holdStart;
    private static boolean directSelected;
    private static int lastDirectDigit = -1;

    /** 上一次观察到的状态，用来判断“服务端确认了切换” */
    private static int observedSlot = -1;
    private static int observedSelected = -1;
    /** 本地请求限频，和服务端的 50ms 保持一致 */
    private static long lastRequestAt;

    public static void initialize() {
        switchKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.update-ft.quiver_next",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyMapping.Category.GAMEPLAY));

        ClientTickEvents.END_CLIENT_TICK.register(QuiverClient::tick);
        // 画在所有原版 HUD 之后：位置在准星右下方，不会和快捷栏/聊天重叠
        HudElementRegistry.addLast(UpdateFt.id("quiver_hud"), QuiverHud::render);

        // 服务端推回的权威内容：界面开着时背包菜单不广播，只能靠这个包对齐
        ClientPlayNetworking.registerGlobalReceiver(QuiverSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    LocalPlayer player = context.client().player;
                    if (player == null) {
                        return;
                    }
                    ItemStack stack = player.getInventory().getItem(payload.inventorySlot());
                    if (QuiverHelper.isQuiver(stack)) {
                        payload.contents().writeTo(stack);
                    }
                }));
    }

    /** 玩家正拿着弓或弩（HUD 常驻、滚轮拦截都以这个为前提） */
    public static boolean isHoldingWeapon(LocalPlayer player) {
        return isWeapon(player.getMainHandItem()) || isWeapon(player.getOffhandItem());
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem;
    }

    /** 正在拉弓 / 给弩上弦：滚轮这时才归箭袋管 */
    private static boolean isDrawing(LocalPlayer player) {
        if (!player.isUsingItem()) {
            return false;
        }
        return player.getUseItem().getItem() instanceof BowItem
                || player.getUseItem().getItem() instanceof CrossbowItem;
    }

    /**
     * 滚轮拦截。返回 true 表示这一下滚动已经被箭袋吃掉（不再切快捷栏）。
     * <p>
     * 方向沿用原版快捷栏的习惯：往上滚是“上一个”，往下滚是“下一个”。
     * 即使已经到头的槽也照样吃掉滚动——否则玩家想换箭却把快捷栏切走了。
     */
    public static boolean onScroll(double yOffset) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || yOffset == 0.0D) {
            return false;
        }
        if (!ModConfig.get().quiver.enabled || !isDrawing(player)) {
            return false;
        }
        int slot = QuiverHelper.findEffectiveSlot(player);
        if (slot < 0) {
            return false;
        }
        QuiverContents contents = QuiverHelper.contents(player.getInventory().getItem(slot));
        int target = contents.nextNonEmpty(contents.selected(), yOffset > 0.0D ? -1 : 1);
        if (target != contents.selected()) {
            requestSwitch(slot, target);
        }
        return true;
    }

    /** 按住 V 时吃掉数字键，避免同时切换快捷栏 */
    public static void suppressHotbarKeys() {
        if (switchKey == null || !switchKey.isDown()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null || minecraft.options == null) {
            return;
        }
        if (QuiverHelper.findEffectiveSlot(minecraft.player) < 0) {
            return;
        }
        for (KeyMapping key : minecraft.options.keyHotbarSlots) {
            // 把队列里的“按下”全部消费掉，原版 handleKeybinds 就看不到这次点击了
            while (key.consumeClick()) {
                // 只消费，不动作
            }
        }
    }

    public static void requestSwitch(int inventorySlot, int target) {
        if (target < 0 || target >= QuiverContents.SLOT_COUNT) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getConnection() == null) {
            return;
        }
        // 纯客户端进原版服时通道不存在，send 会抛异常
        if (!ClientPlayNetworking.canSend(QuiverSwitchPayload.TYPE)) {
            return;
        }
        ModConfig.Quiver config = ModConfig.get().quiver;
        if (!config.enabled) {
            return;
        }
        long now = Util.getMillis();
        if (now - lastRequestAt < 50L) {
            return;
        }
        lastRequestAt = now;

        // 本地预测：界面里的高亮和 HUD 立刻跟手；服务端同步包到达后以它为准
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        if (QuiverHelper.isQuiver(stack)) {
            QuiverContents contents = QuiverHelper.contents(stack);
            if (!contents.isSlotEmpty(target) && contents.selected() != target) {
                contents.withSelected(target).writeTo(stack);
            }
        }

        QuiverHud.setPending(target);
        ClientPlayNetworking.send(new QuiverSwitchPayload(inventorySlot, target));
    }

    private static void tick(Minecraft minecraft) {
        QuiverHud.tick();

        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            resetHold();
            return;
        }
        if (!ModConfig.get().quiver.enabled) {
            resetHold();
            return;
        }

        int slot = QuiverHelper.findEffectiveSlot(player);
        if (slot < 0) {
            resetHold();
            observedSlot = -1;
            observedSelected = -1;
            return;
        }

        QuiverContents contents = QuiverHelper.contents(player.getInventory().getItem(slot));
        observeSwitch(player, slot, contents);
        handleSwitchKey(minecraft, slot, contents);
    }

    /**
     * 服务端确认切换后给反馈。只在“当前槽发生变化”时播一次，避免每 tick 都响。
     * <p>
     * 射空自动跳槽也走这里：跳到的可能是另一种箭，不能无声切换。
     */
    private static void observeSwitch(LocalPlayer player, int slot, QuiverContents contents) {
        int selected = contents.selected();
        if (slot != observedSlot) {
            observedSlot = slot;
            observedSelected = selected;
            return;
        }
        if (selected == observedSelected) {
            return;
        }
        observedSelected = selected;
        QuiverHud.clearPending(selected);

        ItemStack ammo = contents.selectedStack();
        if (ammo.isEmpty()) {
            return;
        }
        QuiverHud.flash(net.minecraft.network.chat.Component.translatable(
                "message.update-ft.quiver.switched",
                ammo.getHoverName(),
                ammo.getCount(),
                selected + 1,
                QuiverContents.SLOT_COUNT));
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3F, 1.2F);
    }

    private static void handleSwitchKey(Minecraft minecraft, int slot, QuiverContents contents) {
        boolean down = switchKey.isDown();

        if (down && !holdingKey) {
            holdingKey = true;
            holdStart = Util.getMillis();
            directSelected = false;
            lastDirectDigit = -1;
        }

        if (holdingKey && down) {
            long held = Util.getMillis() - holdStart;
            if (held >= TAP_THRESHOLD_MS) {
                QuiverHud.setBarVisible(true);
            }
            // 按住时 1–5 直选；同一个数字只发一次，避免每 tick 一个包
            Window window = minecraft.getWindow();
            for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
                if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_1 + i) && lastDirectDigit != i) {
                    lastDirectDigit = i;
                    directSelected = true;
                    if (contents.selected() != i) {
                        requestSwitch(slot, i);
                    }
                }
            }
        }

        if (!down && holdingKey) {
            holdingKey = false;
            QuiverHud.setBarVisible(false);
            boolean tap = !directSelected && Util.getMillis() - holdStart <= TAP_THRESHOLD_MS;
            if (tap) {
                int direction = shiftDown(minecraft) ? -1 : 1;
                int target = contents.nextNonEmpty(contents.selected(), direction);
                if (target != contents.selected()) {
                    requestSwitch(slot, target);
                }
            }
        }
    }

    private static boolean shiftDown(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static void resetHold() {
        holdingKey = false;
        directSelected = false;
        lastDirectDigit = -1;
        QuiverHud.setBarVisible(false);
    }

    private QuiverClient() {
    }
}
