// SPDX-License-Identifier: MPL-2.0

package ztqjntm.update.log;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启动/关闭阶段的结构化日志。
 * <p>
 * 每条日志都带阶段名、事件（begin/end/mark/fail）、相对启动时间、线程和绝对时间戳，
 * 关键步骤成对输出，崩溃时最后一条 {@code begin} 就是出事的位置。
 * 另外挂了一个 shutdown hook：JVM 异常退出时也会补一条 {@code unfinished}，方便判断是哪个阶段没走完。
 * <p>
 * 前缀固定为 {@code [boot]}，排查时直接 grep 即可，不会和正常游戏日志混在一起。
 */
public final class StartupLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("FletchingWorks/Boot");
    private static final long T0 = System.nanoTime();
    private static final Map<String, Long> ACTIVE = new ConcurrentHashMap<>();
    private static final AtomicBoolean HOOK_INSTALLED = new AtomicBoolean();

    private static volatile String lastPhase = "none";

    private StartupLog() {
    }

    /** 环境上下文：加载器、Minecraft 版本、客户端/服务端 */
    public static String[] env() {
        String side = FabricLoader.getInstance().getEnvironmentType().name().toLowerCase(Locale.ROOT);
        String mc = FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        String loader = FabricLoader.getInstance().getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        return new String[]{"side=" + side, "mc=" + mc, "loader=" + loader};
    }

    /** 只装一次；正常退出也会打一条，不需要区分 */
    public static void installShutdownHook() {
        if (!HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LOGGER.info(
                "[boot] phase=shutdown event=mark t=+{}s lastPhase={} unfinished={} ts={}",
                seconds(), lastPhase, ACTIVE.keySet(), Instant.now()), "fletching-works-boot-log"));
    }

    public static void begin(String phase, String... context) {
        ACTIVE.put(phase, System.nanoTime());
        lastPhase = phase;
        log("begin", phase, -1L, context);
    }

    public static void end(String phase, String... context) {
        Long start = ACTIVE.remove(phase);
        log("end", phase, start == null ? -1L : System.nanoTime() - start, context);
    }

    /** 一次性事件，没有配对的 end */
    public static void mark(String phase, String... context) {
        lastPhase = phase;
        log("mark", phase, -1L, context);
    }

    /** begin → 执行 → end；抛异常时打 fail 并把异常原样抛回去 */
    public static void run(String phase, Runnable action, String... context) {
        begin(phase, context);
        try {
            action.run();
        } catch (RuntimeException | Error error) {
            fail(phase, error);
            throw error;
        }
        end(phase);
    }

    public static void fail(String phase, Throwable error, String... context) {
        Long start = ACTIVE.remove(phase);
        long elapsed = start == null ? -1L : System.nanoTime() - start;
        LOGGER.error("[boot] phase={} event=fail t=+{}s elapsed={} thread={}{} ts={}",
                phase, seconds(), millis(elapsed), Thread.currentThread().getName(),
                formatContext(context), Instant.now());
        LOGGER.error("[boot] phase={} aborted", phase, error);
    }

    private static void log(String event, String phase, long elapsedNanos, String... context) {
        String elapsed = elapsedNanos < 0L ? "" : " elapsed=" + millis(elapsedNanos);
        LOGGER.info("[boot] phase={} event={} t=+{}s{} thread={}{} ts={}",
                phase, event, seconds(), elapsed,
                Thread.currentThread().getName(), formatContext(context), Instant.now());
    }

    private static String seconds() {
        return String.format(Locale.ROOT, "%.3f", (System.nanoTime() - T0) / 1_000_000_000.0D);
    }

    private static String millis(long nanos) {
        return nanos < 0L ? "-" : String.format(Locale.ROOT, "%.1fms", nanos / 1_000_000.0D);
    }

    private static String formatContext(String... context) {
        return context.length == 0 ? "" : " " + String.join(" ", context);
    }
}
