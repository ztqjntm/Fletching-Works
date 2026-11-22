// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import ztqjntm.update.UpdateFt;
import ztqjntm.update.item.quiver.QuiverHelper;
import ztqjntm.update.menu.QuiverMenu;

/**
 * 箭袋界面。
 * <p>
 * 贴图里 5 个 18×18 槽框在 {@code x = 43, 61, 79, 97, 115 / y = 33}（面板坐标），物品区 +1。
 * 每个槽上方画一个小三角，点它就把该槽设为“当前槽”；{@code Ctrl + 左键} 点槽位是同一动作的快捷方式
 * （原版槽位点击包不带修饰键，所以这里在 Screen 层先拦下来）。
 */
public class QuiverScreen extends AbstractContainerScreen<QuiverMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(UpdateFt.MOD_ID, "textures/gui/quiver.png");

    /** 槽框左上角（面板坐标） */
    private static final int CELL_X = 33;
    private static final int CELL_Y = 33;
    private static final int CELL = 18;
    /** 三角的尖端所在行 */
    private static final int TRIANGLE_Y = 23;
    private static final int TRIANGLE_HEIGHT = 5;

    private static final int COLOR_TRIANGLE = 0xFF9E9E9E;
    private static final int COLOR_TRIANGLE_CURRENT = 0xFFFFD700;
    private static final int COLOR_TRIANGLE_HOVER = 0xFFFFFFFF;
    private static final int COLOR_CELL_CURRENT = 0xFFFFD700;

    public QuiverScreen(QuiverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight,
                BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);

        int selected = this.menu.selected();
        for (int i = 0; i < QuiverMenu.SLOT_COUNT; i++) {
            int cellX = x + CELL_X + i * CELL;
            int cellY = y + CELL_Y;
            if (i == selected) {
                outline(graphics, cellX, cellY, CELL, CELL, COLOR_CELL_CURRENT);
            }
            boolean hovered = insideTriangle(mouseX, mouseY, i);
            int color = i == selected ? COLOR_TRIANGLE_CURRENT
                    : hovered ? COLOR_TRIANGLE_HOVER : COLOR_TRIANGLE;
            drawTriangle(graphics, triangleCenterX(i), y + TRIANGLE_Y, color);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);

        // 打开的不是生效箭袋时给一句提示：否则玩家会问“为什么我装满了却不用这个”
        if (!this.isEffectiveQuiver()) {
            graphics.drawString(this.font,
                    Component.translatable("gui.update-ft.quiver.inactive"),
                    this.leftPos + 8, this.topPos + 62, 0xFF9E9E9E, false);
        }

        for (int i = 0; i < QuiverMenu.SLOT_COUNT; i++) {
            if (insideTriangle(mouseX, mouseY, i)) {
                graphics.setTooltipForNextFrame(this.font,
                        Component.translatable("gui.update-ft.quiver.select"), mouseX, mouseY);
                break;
            }
        }
    }

    /** 生效箭袋 = 快捷栏/背包里最靠前的那个；这里只用来决定提不提示 */
    private boolean isEffectiveQuiver() {
        return this.minecraft.player != null
                && QuiverHelper.findEffectiveSlot(this.minecraft.player) == this.menu.inventorySlot();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (int i = 0; i < QuiverMenu.SLOT_COUNT; i++) {
                if (insideTriangle(event.x(), event.y(), i)) {
                    QuiverClient.requestSwitch(this.menu.inventorySlot(), i);
                    return true;
                }
            }
            // Ctrl + 左键点槽位 = 设为当前槽。必须在 super 之前拦，否则会被当成普通取放
            if (event.hasControlDown() && this.hoveredSlot != null
                    && this.hoveredSlot.index < QuiverMenu.SLOT_COUNT) {
                QuiverClient.requestSwitch(this.menu.inventorySlot(), this.hoveredSlot.index);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int triangleCenterX(int index) {
        return this.leftPos + CELL_X + index * CELL + CELL / 2;
    }

    private boolean insideTriangle(double mouseX, double mouseY, int index) {
        int cx = triangleCenterX(index);
        int top = this.topPos + TRIANGLE_Y;
        return mouseX >= cx - 6 && mouseX <= cx + 6
                && mouseY >= top - 1 && mouseY <= top + TRIANGLE_HEIGHT + 1;
    }

    /** 尖端朝下的实心三角：逐行收窄，5 行刚好占 5px */
    private static void drawTriangle(GuiGraphics graphics, int centerX, int top, int color) {
        for (int row = 0; row < TRIANGLE_HEIGHT; row++) {
            int half = TRIANGLE_HEIGHT - 1 - row;
            graphics.fill(centerX - half, top + row, centerX + half + 1, top + row + 1, color);
        }
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
