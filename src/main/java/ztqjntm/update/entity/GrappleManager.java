package ztqjntm.update.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 钩爪箭的逐 tick 牵引。
 * <p>
 * 单次冲量只能跳 3–4 格，射 15 格外的墙根本到不了，所以这里每 tick 移动 0.8 格，
 * 最多 40 tick（32 格 ÷ 0.8 格/tick），撞墙就停。玩家每 tick 补 {@link ClientboundSetEntityMotionPacket}，
 * 否则客户端会用自己的速度覆盖服务端位置。
 * <p>
 * 会话只存服务端实体引用；玩家退出、死亡、换维度、被移除时立即清理。
 */
public final class GrappleManager {

    /** 每 tick 移动距离（格） */
    private static final double SPEED = 0.8D;
    /** 最长牵引时间（tick）：32 格 / 0.8 格每 tick = 40 tick，留够射满 32 格的时间 */
    private static final int MAX_TICKS = 40;
    /** 距离锚点小于这个值就结束，避免贴脸抖动 */
    private static final double STOP_DISTANCE = 1.0D;

    private static final List<Session> SESSIONS = new ArrayList<>();

    private GrappleManager() {
    }

    /** 开始一段牵引；同一个实体已有会话时先取消旧的，避免两段会话每 tick 互相覆盖 */
    public static void start(ServerLevel level, Entity subject, Vec3 anchor) {
        SESSIONS.removeIf(session -> session.subject == subject);
        SESSIONS.add(new Session(level, subject, anchor));
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GrappleManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    private static void tick(MinecraftServer server) {
        Iterator<Session> iterator = SESSIONS.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            Entity subject = session.subject;

            if (subject.isRemoved()
                    || subject.level() != session.level
                    || (subject instanceof ServerPlayer player && player.hasDisconnected())
                    || (subject instanceof LivingEntity living && living.isDeadOrDying())) {
                iterator.remove();
                continue;
            }
            if (++session.ticks > MAX_TICKS) {
                iterator.remove();
                continue;
            }

            Vec3 current = subject.position();
            Vec3 toAnchor = session.anchor.subtract(current);
            double distance = toAnchor.length();
            if (distance < STOP_DISTANCE) {
                iterator.remove();
                continue;
            }

            Vec3 step = toAnchor.normalize().scale(Math.min(SPEED, distance));
            AABB nextBox = subject.getBoundingBox().move(step);
            if (!session.level.noCollision(subject, nextBox)) {
                iterator.remove();
                continue;
            }
            if (!subject.teleportTo(session.level,
                    current.x + step.x, current.y + step.y, current.z + step.z,
                    Set.of(), subject.getYRot(), subject.getXRot(), false)) {
                iterator.remove();
                continue;
            }

            subject.resetFallDistance();
            subject.setDeltaMovement(Vec3.ZERO);
            if (subject instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
        }
    }

    private static final class Session {
        private final ServerLevel level;
        private final Entity subject;
        private final Vec3 anchor;
        private int ticks;

        private Session(ServerLevel level, Entity subject, Vec3 anchor) {
            this.level = level;
            this.subject = subject;
            this.anchor = anchor;
        }
    }
}
