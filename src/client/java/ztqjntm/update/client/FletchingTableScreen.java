// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ztqjntm.update.UpdateFt;
import ztqjntm.update.menu.FletchingTableMenu;
import ztqjntm.update.recipe.FletchingRecipes;
import ztqjntm.update.tag.ModTags;

import java.util.ArrayList;
import java.util.List;

public class FletchingTableScreen extends AbstractContainerScreen<FletchingTableMenu> {
	// 制箭台自己的贴图：256×256 的画布，176×166 的面板画在左上角 (0,0)。
	// 原料格 / 产物格 / 背包的坐标都在 FletchingTableMenu 里，改贴图时两边要一起改。
	private static final Identifier CONTAINER_TEXTURE =
			Identifier.fromNamespaceAndPath(UpdateFt.MOD_ID, "textures/gui/ft_gui.png");

	/** 空槽幽灵图标上盖一层接近槽位底色的半透明灰，让物品看起来是“提示”而不是真物品 */
	private static final int GHOST_WASH = 0xA08B8B8B;

	public FletchingTableScreen(FletchingTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		// 标题居中
		this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
		// renderBg 不在 translate(leftPos, topPos) 的矩阵里，面板坐标必须自己加偏移
		int x = this.leftPos;
		int y = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE,
				x, y, 0.0F, 0.0F,
				this.imageWidth, this.imageHeight,
				BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);

		// 空槽提示：一眼看出三个格子该放什么，不用装 JEI 或翻 README
		drawGhost(graphics, x, y, FletchingRecipes.TIP_SLOT, currentTip());
		drawGhost(graphics, x, y, FletchingRecipes.SHAFT_SLOT, new ItemStack(Items.STICK));
		drawGhost(graphics, x, y, FletchingRecipes.FLETCHING_SLOT, new ItemStack(Items.FEATHER));
	}

	private void drawGhost(GuiGraphics graphics, int originX, int originY, int index, ItemStack hint) {
		if (hint.isEmpty() || !this.menu.getSlot(index).getItem().isEmpty()) {
			return;
		}
		int slotX = originX + this.menu.getSlot(index).x;
		int slotY = originY + this.menu.getSlot(index).y;
		graphics.renderItem(hint, slotX, slotY);
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, GHOST_WASH);
	}

	/** 箭头格每秒轮播一种可用材料；标签没同步/为空时退回燧石 */
	private static ItemStack currentTip() {
		List<Item> tips = new ArrayList<>();
		BuiltInRegistries.ITEM.getTagOrEmpty(ModTags.FLETCHING_TIPS).forEach(holder -> tips.add(holder.value()));
		if (tips.isEmpty()) {
			return new ItemStack(Items.FLINT);
		}
		int index = (int) ((Util.getMillis() / 1000L) % tips.size());
		return new ItemStack(tips.get(index));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		this.renderTooltip(graphics, mouseX, mouseY);
		renderSlotHint(graphics, mouseX, mouseY);
	}

	/** 空槽悬停时给一句说明，配合幽灵图标 */
	private void renderSlotHint(GuiGraphics graphics, int mouseX, int mouseY) {
		if (this.hoveredSlot == null
				|| this.hoveredSlot.index >= FletchingTableMenu.INPUT_SIZE
				|| !this.hoveredSlot.getItem().isEmpty()) {
			return;
		}
		Component hint = Component.translatable("gui.update-ft.slot_hint." + this.hoveredSlot.index);
		graphics.setTooltipForNextFrame(this.font, hint, mouseX, mouseY);
	}
}
