// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.mixin.client;

import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.state.TippableArrowRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ztqjntm.update.client.ArrowFlightState;

/**
 * 普通箭（{@code Arrow}）用的渲染器。状态里带自定义贴图时优先用它，
 * 否则保持原版：普通箭 → {@code arrow.png}，药箭 → {@code arrow_tipped.png}。
 */
@Mixin(TippableArrowRenderer.class)
public abstract class TippableArrowRendererMixin {

    @Inject(
            method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/TippableArrowRenderState;)"
                    + "Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void updateft$flightTexture(TippableArrowRenderState state, CallbackInfoReturnable<Identifier> cir) {
        Identifier texture = ((ArrowFlightState) state).updateft$getFlightTexture();
        if (texture != null) {
            cir.setReturnValue(texture);
        }
    }
}
