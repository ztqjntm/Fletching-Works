// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import org.junit.jupiter.api.Test;
import ztqjntm.update.GameTestBootstrap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证同步字段真的挂进了 {@code AbstractArrow}，并且 ID 没和原版 / {@code Arrow} 子类撞号。
 * <p>
 * 这是这套渲染方案最容易悄悄失效的地方：字段必须在 {@code AbstractArrow} 的静态初始化里
 * 分配 ID，晚于 {@code Arrow} 的 {@code ID_EFFECT_COLOR} 就会撞号。
 */
class ArrowTypeSyncTest {

    @Test
    void syncFieldIsRegisteredWithoutIdCollision() throws Exception {
        GameTestBootstrap.bootstrap();

        Class<?> abstractArrow = Class.forName("net.minecraft.world.entity.projectile.arrow.AbstractArrow");
        assertTrue(ArrowTypeHolder.class.isAssignableFrom(abstractArrow),
                "AbstractArrowMixin 没有把 ArrowTypeHolder 挂上去");

        Set<Integer> ids = new HashSet<>();
        int accessors = 0;
        for (Field field : abstractArrow.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    || field.getType() != EntityDataAccessor.class) {
                continue;
            }
            field.setAccessible(true);
            EntityDataAccessor<?> accessor = (EntityDataAccessor<?>) field.get(null);
            assertNotNull(accessor, field.getName());
            assertTrue(ids.add(accessor.id()), "重复的实体数据 ID: " + field.getName());
            accessors++;
        }

        // 原版 3 个（flags / pierce / inGround）+ 我们 1 个
        assertEquals(4, accessors, "AbstractArrow 的实体数据字段数量不对，mixin 可能没生效");
        // Entity 自己已经占了 0–7，所以这里只检查“四个字段彼此不撞号”，不硬编码起始值
        assertEquals(4, ids.size());
        int maxId = ids.stream().mapToInt(Integer::intValue).max().orElseThrow();

        Class<?> arrow = Class.forName("net.minecraft.world.entity.projectile.arrow.Arrow");
        Field colorField = arrow.getDeclaredField("ID_EFFECT_COLOR");
        colorField.setAccessible(true);
        EntityDataAccessor<?> color = (EntityDataAccessor<?>) colorField.get(null);
        assertEquals(maxId + 1, color.id(), "Arrow 的颜色字段应该排在我们后面");
    }
}
