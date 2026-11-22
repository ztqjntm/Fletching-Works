// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.network.chat.Component;
import ztqjntm.update.block.ModBlocks;
import ztqjntm.update.client.jei.SyncedFletchingRecipes;
import ztqjntm.update.item.QuiverItem;
import ztqjntm.update.item.quiver.QuiverHelper;
import ztqjntm.update.log.StartupLog;
import ztqjntm.update.menu.ModMenuTypes;

public class UpdateFtClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		StartupLog.run("client-init", () -> {
			// Screen 只在客户端存在，必须在这里注册
			MenuScreens.register(ModMenuTypes.FLETCHING_TABLE, FletchingTableScreen::new);
			MenuScreens.register(ModMenuTypes.QUIVER, QuiverScreen::new);

			// 箭袋：V 循环 / 滚轮切换 / HUD
			QuiverClient.initialize();

			// “当前生效”只能在这里加：Item#appendHoverText 拿不到玩家，判断不了哪一个是生效箭袋。
			// 用引用比较：只有背包里那个真实的 stack 才标，创造模式页签/容器里的副本不标。
			ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
				if (!(stack.getItem() instanceof QuiverItem)) {
					return;
				}
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null || QuiverHelper.effectiveQuiver(minecraft.player) != stack) {
					return;
				}
				lines.add(Component.translatable("tooltip.update-ft.quiver.active")
						.withStyle(ChatFormatting.GOLD));
			});

			// 临时蛛网用的是原版 block/cobweb 模型，模型本身不带渲染层信息；
			// 不注册 CUTOUT 就会按 SOLID 渲染，透明像素变黑，看起来像贴图坏了。
			BlockRenderLayerMap.putBlock(ModBlocks.TEMPORARY_COBWEB, ChunkSectionLayer.CUTOUT);

			// 1.21.11 的客户端没有配方表：配方由服务端按序列化器同步过来，
			// 这里缓存一份给 JEI 用（同时处理 /reload 后的重新同步）。
			ClientRecipeSynchronizedEvent.EVENT.register(
					(client, synchronizedRecipes) -> SyncedFletchingRecipes.accept(synchronizedRecipes));
			ClientPlayConnectionEvents.DISCONNECT.register(
					(handler, client) -> SyncedFletchingRecipes.clear());

			// 首帧之后才算“客户端真正起来了”，用来给启动耗时收尾
			ClientLifecycleEvents.CLIENT_STARTED.register(client ->
					StartupLog.mark("client-ready",
							"window=" + client.getWindow().getWidth() + "x" + client.getWindow().getHeight()));
			ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
					StartupLog.mark("client-stopping"));
		}, StartupLog.env());
	}
}
