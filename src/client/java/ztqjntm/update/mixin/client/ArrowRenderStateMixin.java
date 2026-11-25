// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.mixin.client;

import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ztqjntm.update.client.ArrowFlightState;

/**
 * 给原版渲染状态加一个“自定义贴图”字段。
 * <p>
 * 渲染状态每帧复用，所以 {@code extract} 里每次都要写（没配图时写 null），
 * 不能只在有值时写，否则上一帧的贴图会粘在下一支箭上。
 */
@Mixin(ArrowRenderState.class)
public abstract class ArrowRenderStateMixin implements ArrowFlightState {

    @Unique
    private Identifier updateft$flightTexture;

    @Override
    public void updateft$setFlightTexture(Identifier texture) {
        this.updateft$flightTexture = texture;
    }

    @Override
    public Identifier updateft$getFlightTexture() {
        return this.updateft$flightTexture;
    }
}
