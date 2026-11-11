package ztqjntm.update.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 蛛网箭留下的临时蛛网：8 秒（160 tick）后自己消失。
 * <p>
 * 用计划刻而不是全局 Map：计划刻会随存档保存，服务器重启后不会留下一张永久蛛网。
 * 行为完全继承原版 {@link WebBlock}（减速、无碰撞、可燃），只是多了一个到期删除。
 */
public class TemporaryCobwebBlock extends WebBlock {
    public static final MapCodec<TemporaryCobwebBlock> CODEC = simpleCodec(TemporaryCobwebBlock::new);

    public TemporaryCobwebBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<WebBlock> codec() {
        // WebBlock#codec 的返回类型是 MapCodec<WebBlock>，这里做一次安全的窄化转换。
        return CODEC.xmap(block -> block, block -> (TemporaryCobwebBlock) block);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 被别的方块替换掉之后计划刻仍会跑一次，这里确认还是自己再删。
        if (state.is(this)) {
            level.removeBlock(pos, false);
        }
    }
}
