// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.log;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * 服务端生命周期日志：世界加载、启动完成、重载数据包、停服。
 * <p>
 * 与 {@link StartupLog} 配合，把“服务端从开始加载到可用”拆成一段可计时的 phase；
 * 世界加载是最容易因为数据包/区块/模组冲突而崩的阶段，begin 之后没有 end 就说明卡在这里。
 */
public final class BootDiagnostics {

    private BootDiagnostics() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                StartupLog.begin("server-start", "world=" + server.getWorldData().getLevelName()));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            StartupLog.end("server-start",
                    "dimensions=" + server.levelKeys().size(),
                    "recipes=" + server.getRecipeManager().getRecipes().size(),
                    "advancements=" + server.getAdvancements().getAllAdvancements().size(),
                    "tick=" + server.getTickCount());
            StartupLog.mark("server-ready", "tick=" + server.getTickCount());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                StartupLog.begin("server-stop", "tick=" + server.getTickCount()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> StartupLog.end("server-stop"));

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) ->
                StartupLog.mark("datapack-reload",
                        "success=" + success,
                        "recipes=" + server.getRecipeManager().getRecipes().size(),
                        "advancements=" + server.getAdvancements().getAllAdvancements().size()));
    }
}
