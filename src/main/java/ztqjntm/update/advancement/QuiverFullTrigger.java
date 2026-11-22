// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * 进度触发器：箭袋装满（每个槽都达到每槽上限）。
 * <p>
 * 原版的 {@code inventory_changed} 只能按物品/数量匹配，读不到组件里的六个槽，所以这里必须
 * 自定义一个触发器。条件为空：只要触发一次就算达成，具体“满没满”由调用方（{@code QuiverHelper}）
 * 判断。
 */
public class QuiverFullTrigger extends SimpleCriterionTrigger<QuiverFullTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));
    }
}
