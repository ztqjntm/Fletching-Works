// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.entity;

/**
 * 箭实体同步下来的“这一发是哪个物品”。
 * <p>
 * 原版 {@code AbstractArrow.pickupItemStack} 只保存在服务端（不写入
 * {@code SynchedEntityData}），客户端箭实体拿到的是空栈；药箭之所以能在客户端变色，
 * 是因为它单独同步了一个 {@code ID_EFFECT_COLOR}。渲染贴图也需要同样的同步，
 * 由 {@code AbstractArrowMixin} 往实体数据里加一个物品 ID 字符串实现。
 */
public interface ArrowTypeHolder {

    /** 服务端写入的物品 ID（如 {@code update-ft:tnt_arrow}）；没有同步到时是空串 */
    String updateft$getArrowTypeId();
}
