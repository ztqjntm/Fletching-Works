// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ztqjntm.update.client.QuiverClient;

/**
 * 拉弓时的滚轮归箭袋管。
 * <p>
 * 原版滚轮会切快捷栏，而切快捷栏会**取消拉弓**——这正是箭袋要解决的问题。这里只在
 * “手里拿着弓/弩并且正在使用”时拦截，其余时间完全交给原版；打开界面时也不拦。
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void updateft$quiverScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (yOffset != 0.0D && QuiverClient.onScroll(yOffset)) {
            ci.cancel();
        }
    }
}
