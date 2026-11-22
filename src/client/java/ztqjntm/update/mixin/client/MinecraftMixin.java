// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ztqjntm.update.client.QuiverClient;

/**
 * 按住 V 时把数字键 1–5 从“切快捷栏”改成“直选箭袋槽位”。
 * <p>
 * 必须在 {@code handleKeybinds} 之前消费掉点击队列：否则原版会先切快捷栏，
 * 而切快捷栏会取消拉弓——正好毁掉箭袋最主要的用法。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void updateft$suppressHotbarKeys(CallbackInfo ci) {
        QuiverClient.suppressHotbarKeys();
    }
}
