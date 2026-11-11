package ztqjntm.update.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import ztqjntm.update.block.ModBlocks;
import ztqjntm.update.menu.ModMenuTypes;

public class UpdateFtClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Screen 只在客户端存在，必须在这里注册
		MenuScreens.register(ModMenuTypes.FLETCHING_TABLE, FletchingTableScreen::new);

		// 临时蛛网用的是原版 block/cobweb 模型，模型本身不带渲染层信息；
		// 不注册 CUTOUT 就会按 SOLID 渲染，透明像素变黑，看起来像贴图坏了。
		BlockRenderLayerMap.putBlock(ModBlocks.TEMPORARY_COBWEB, ChunkSectionLayer.CUTOUT);
	}
}
