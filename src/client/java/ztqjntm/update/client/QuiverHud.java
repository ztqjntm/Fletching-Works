// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import ztqjntm.update.config.ModConfig;
import ztqjntm.update.item.quiver.QuiverContents;
import ztqjntm.update.item.quiver.QuiverHelper;

/**
 * 准星右下方的箭袋 HUD：当前箭图标 + 数量 + 6 个点阵（+ 槽位序号）。
 * <p>
 * 只读服务端同步下来的组件，不做任何预测性扣减；切换请求发出后，目标点先画成“待确认”，
 * 等组件真的变了才换音效和浮字。
 */
public final class QuiverHud {

    private static final int FLASH_TICKS = 30;      // 1.5s
    private static final int PENDING_TIMEOUT_MS = 750;

    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xFF9E9E9E;
    private static final int COLOR_DOT = 0xFFFFFFFF;
    private static final int COLOR_DOT_EMPTY = 0x55FFFFFF;
    private static final int COLOR_CURRENT = 0xFFFFD700;
    private static final int COLOR_PENDING = 0xFF00E5FF;

    private static Component flashMessage;
    private static int flashTicks;

    private static int pendingTarget = -1;
    private static long pendingAt;

    private static boolean barVisible;

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        if (!ModConfig.get().quiver.enabled || !ModConfig.get().quiver.hud) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        // 手持弓弩时常驻；切换后 / 按住 V 时单独显示 1.5 秒
        boolean holding = QuiverClient.isHoldingWeapon(player);
        if (!holding && flashTicks <= 0 && !barVisible) {
            return;
        }

        int quiverSlot = QuiverHelper.findEffectiveSlot(player);
        if (quiverSlot < 0) {
            return;
        }
        ItemStack quiver = player.getInventory().getItem(quiverSlot);
        QuiverContents contents = QuiverHelper.contents(quiver);

        ItemStack ammo = contents.selectedStack();
        boolean fallback = false;
        if (ammo.isEmpty()) {
            ammo = findInventoryAmmo(player, quiverSlot);
            fallback = !ammo.isEmpty();
            if (ammo.isEmpty()) {
                return;
            }
        }

        ModConfig.Quiver config = ModConfig.get().quiver;
        int x = graphics.guiWidth() / 2 + config.hudOffsetX;
        int y = graphics.guiHeight() / 2 + config.hudOffsetY;

        graphics.renderItem(ammo, x, y);
        if (player.getCooldowns().isOnCooldown(ammo)) {
            // 钩爪箭这类物品冷却：叠一层暗色遮罩，和原版快捷栏的观感一致
            graphics.fill(x, y, x + 16, y + 16, 0x99000000);
        }

        String count = String.valueOf(ammo.getCount());
        graphics.drawString(minecraft.font, count, x + 18, y + 1, COLOR_TEXT, true);

        if (fallback) {
            // 箭袋空、改用背包箭：只显示“背包”角标，不画点阵（点阵会让人以为箭袋里还有）
            graphics.drawString(minecraft.font,
                    Component.translatable("tooltip.update-ft.quiver.backpack")
                            .withStyle(ChatFormatting.GRAY),
                    x + 18, y + 10, COLOR_DIM, true);
            drawFlash(graphics, minecraft, x, y);
            return;
        }

        String index = (contents.selected() + 1) + "/" + QuiverContents.SLOT_COUNT;
        graphics.drawString(minecraft.font, index, x + 18, y + 10, COLOR_DIM, true);

        // 6 个点：实心 = 有箭，空心 = 空槽，当前槽用金色外框
        int dotX = x + 40;
        int dotY = y + 5;
        for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
            int px = dotX + i * 7;
            boolean filled = !contents.isSlotEmpty(i);
            graphics.fill(px, dotY, px + 3, dotY + 3, filled ? COLOR_DOT : COLOR_DOT_EMPTY);
            if (i == contents.selected()) {
                outline(graphics, px - 1, dotY - 1, 5, 5, COLOR_CURRENT);
            }
            if (i == pendingTarget) {
                outline(graphics, px - 2, dotY - 2, 7, 7, COLOR_PENDING);
            }
        }

        if (barVisible) {
            drawBar(graphics, minecraft, contents, x, y + 22);
        }

        // 拉弓锁定时给一个角标：此时 HUD 显示的是“下一发”，不是正在射出的那一发
        if (config.lockOnDraw && player.isUsingItem() && flashTicks <= 0) {
            graphics.drawString(minecraft.font,
                    Component.translatable("hud.update-ft.quiver.locked"),
                    x, y - 12, COLOR_CURRENT, true);
        }

        drawFlash(graphics, minecraft, x, y);
    }

    /** 按住 V 时的槽位条：5 个 18×18 格子，当前槽金框 */
    private static void drawBar(GuiGraphics graphics, Minecraft minecraft, QuiverContents contents, int x, int y) {
        int width = QuiverContents.SLOT_COUNT * 18;
        graphics.fill(x - 3, y - 3, x + width + 3, y + 18 + 3, 0x99000000);
        for (int i = 0; i < QuiverContents.SLOT_COUNT; i++) {
            int cellX = x + i * 18;
            ItemStack stack = contents.get(i);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, cellX + 1, y + 1);
            }
            if (i == contents.selected()) {
                outline(graphics, cellX, y, 18, 18, COLOR_CURRENT);
            }

        }
    }

    private static void drawFlash(GuiGraphics graphics, Minecraft minecraft, int x, int y) {
        if (flashTicks <= 0 || flashMessage == null) {
            return;
        }
        graphics.drawString(minecraft.font, flashMessage, x, y - 12, COLOR_TEXT, true);
    }

    /** 箭袋空时，按当前武器找到背包里第一叠能用的箭（跳过箭袋自己那一格） */
    private static ItemStack findInventoryAmmo(LocalPlayer player, int quiverSlot) {
        ProjectileWeaponItem weapon = heldWeapon(player);
        if (weapon == null) {
            return ItemStack.EMPTY;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (i == quiverSlot) {
                continue;
            }
            ItemStack stack = inventory.getItem(i);
            if (weapon.getAllSupportedProjectiles().test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ProjectileWeaponItem heldWeapon(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof ProjectileWeaponItem weapon) {
            return weapon;
        }
        if (player.getOffhandItem().getItem() instanceof ProjectileWeaponItem weapon) {
            return weapon;
        }
        return null;
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static void flash(Component message) {
        flashMessage = message;
        flashTicks = FLASH_TICKS;
    }

    public static void setPending(int target) {
        pendingTarget = target;
        pendingAt = Util.getMillis();
    }

    /** 服务端确认到目标槽后立刻收掉“待确认”框，不用等超时 */
    public static void clearPending(int selected) {
        if (pendingTarget == selected) {
            pendingTarget = -1;
        }
    }

    public static void setBarVisible(boolean visible) {
        barVisible = visible;
    }

    public static void tick() {
        if (flashTicks > 0) {
            flashTicks--;
        }
        if (pendingTarget >= 0 && Util.getMillis() - pendingAt > PENDING_TIMEOUT_MS) {
            pendingTarget = -1;
        }
    }

    private QuiverHud() {
    }
}
