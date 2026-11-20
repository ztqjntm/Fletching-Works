// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * 单测启动：初始化原版注册表。
 * <p>
 * 单测环境没有数据包，物品标签不会被绑定；涉及标签的代码统一走 {@link ztqjntm.update.tag.ModTags#isIn}
 * （未绑定时返回 false），所以这里不需要额外准备标签数据。
 */
public final class GameTestBootstrap {
    private static boolean bootstrapped;

    private GameTestBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bootstrapped = true;
    }
}
