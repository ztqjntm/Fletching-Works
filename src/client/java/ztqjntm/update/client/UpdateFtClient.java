package ztqjntm.update.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import ztqjntm.update.block.ModBlocks;
import ztqjntm.update.client.jei.SyncedFletchingRecipes;
import ztqjntm.update.menu.ModMenuTypes;

public class UpdateFtClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Screen 只在客户端存在，必须在这里注册
		MenuScreens.register(ModMenuTypes.FLETCHING_TABLE, FletchingTableScreen::new);

		// 临时蛛网用的是原版 block/cobweb 模型，模型本身不带渲染层信息；
		// 不注册 CUTOUT 就会按 SOLID 渲染，透明像素变黑，看起来像贴图坏了。
		BlockRenderLayerMap.putBlock(ModBlocks.TEMPORARY_COBWEB, ChunkSectionLayer.CUTOUT);

		// 1.21.11 的客户端没有配方表：配方由服务端按序列化器同步过来，
		// 这里缓存一份给 JEI 用（同时处理 /reload 后的重新同步）。
		ClientRecipeSynchronizedEvent.EVENT.register(
				(client, synchronizedRecipes) -> SyncedFletchingRecipes.accept(synchronizedRecipes));
		ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> SyncedFletchingRecipes.clear());
	}
}
