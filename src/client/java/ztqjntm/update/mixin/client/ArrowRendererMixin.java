// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.mixin.client;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ztqjntm.update.client.ArrowFlightState;
import ztqjntm.update.client.ArrowFlightTextures;

/**
 * 飞行箭的分流点：在 extract 阶段把“这一发是哪个物品”翻译成贴图，存进渲染状态。
 * <p>
 * 选 {@code extractRenderState} 而不是 {@code getTextureLocation}，是因为 1.21.11 的
 * {@code getTextureLocation} 只收渲染状态、拿不到实体；而 {@code extract} 同时有实体和状态。
 * 写在 {@code ArrowRenderer} 基类上，{@code TippableArrowRenderer} 的覆写会先调 {@code super}，
 * 所以普通箭同样会经过这里。
 */
@Mixin(ArrowRenderer.class)
public abstract class ArrowRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;"
                    + "Lnet/minecraft/client/renderer/entity/state/ArrowRenderState;F)V",
            at = @At("TAIL")
    )
    private void updateft$setFlightTexture(AbstractArrow arrow, ArrowRenderState state, float partialTick,
                                           CallbackInfo ci) {
        ((ArrowFlightState) state).updateft$setFlightTexture(ArrowFlightTextures.textureFor(arrow));
    }
}
