// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.menu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import ztqjntm.update.UpdateFt;

public class ModMenuTypes {
	// MenuType 的构造函数在原版是 protected，
	// Fabric 的 transitive access widener 让它在开发环境可直接 new。
	public static final MenuType<FletchingTableMenu> FLETCHING_TABLE =
			Registry.register(
					BuiltInRegistries.MENU,
					UpdateFt.id("fletching_table"),
					new MenuType<>(FletchingTableMenu::new, FeatureFlags.VANILLA_SET)
			);

	/**
	 * 箭袋界面。用扩展菜单类型把“箭袋在哪个背包槽”作为 int 带给客户端：
	 * 原版 {@code openMenu(MenuProvider)} 没有附带数据的重载，客户端否则只能猜
	 * “最靠前的箭袋”，打开第二个箭袋时就会编辑错物品。
	 */
	public static final MenuType<QuiverMenu> QUIVER =
			Registry.register(
					BuiltInRegistries.MENU,
					UpdateFt.id("quiver"),
					new ExtendedScreenHandlerType<QuiverMenu, Integer>(
							(syncId, inventory, slot) -> new QuiverMenu(syncId, inventory, slot),
							ByteBufCodecs.VAR_INT)
			);

	// 和 ModItems.initialize() 一样：空方法，只为触发 static 字段初始化
	public static void initialize() {
	}
}
