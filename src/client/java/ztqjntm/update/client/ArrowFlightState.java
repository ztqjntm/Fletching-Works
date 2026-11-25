// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.client;

import net.minecraft.resources.Identifier;

/**
 * 给 {@code ArrowRenderState} 挂一个“这一帧用哪张贴图”的槽位。
 * <p>
 * 1.21.11 的实体渲染拆成了 extract（实体 → 渲染状态）和 submit（渲染状态 → 画面）两步，
 * {@code getTextureLocation} 只能拿到渲染状态、拿不到实体。所以贴图要在 extract 阶段算好、
 * 存进状态里，submit 阶段再读出来。
 */
public interface ArrowFlightState {

    void updateft$setFlightTexture(Identifier texture);

    Identifier updateft$getFlightTexture();
}
