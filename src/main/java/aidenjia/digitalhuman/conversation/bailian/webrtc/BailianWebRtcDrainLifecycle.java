package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

/**
 * 优雅下线：标记 draining → 广播 server.draining → 等待宽限 → 关闭 WebRTC 控制会话。
 *
 * <p>保留既有 phase；Spring 按 phase 从高到低停止组件。
 */
@Component
public class BailianWebRtcDrainLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(BailianWebRtcDrainLifecycle.class);

    private final BailianRealtimeEventsRegistry eventsRegistry;
    private final BailianRealtimeProperties properties;
    private final Executor drainExecutor;

    private final AtomicBoolean draining = new AtomicBoolean(false);
    private volatile boolean running = false;

    @Autowired
    public BailianWebRtcDrainLifecycle(
            BailianRealtimeEventsRegistry eventsRegistry, BailianRealtimeProperties properties) {
        this(
                eventsRegistry,
                properties,
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread t = new Thread(r, "bailian-realtime-drain");
                            t.setDaemon(true);
                            return t;
                        }));
    }

    BailianWebRtcDrainLifecycle(
            BailianRealtimeEventsRegistry eventsRegistry,
            BailianRealtimeProperties properties,
            Executor drainExecutor) {
        this.eventsRegistry = eventsRegistry;
        this.properties = properties;
        this.drainExecutor = drainExecutor;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public void stop(Runnable callback) {
        if (!draining.compareAndSet(false, true)) {
            callback.run();
            return;
        }
        long graceMs = Math.max(0, properties.getDrainGraceSeconds()) * 1000L;
        log.info(
                "Bailian realtime drain begin activeSessions={} graceMs={}",
                eventsRegistry.all().size(),
                graceMs);
        drainExecutor.execute(
                () -> {
                    try {
                        for (BailianRealtimeEventsSession session : eventsRegistry.all()) {
                            try {
                                session.sendDraining();
                            } catch (Exception ex) {
                                log.warn("send events draining failed");
                            }
                        }
                        try {
                            if (graceMs > 0) {
                                Thread.sleep(graceMs);
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        for (BailianRealtimeEventsSession session : eventsRegistry.all()) {
                            session.closeBoth(CloseStatus.GOING_AWAY);
                        }
                        log.info("Bailian realtime drain complete");
                    } finally {
                        running = false;
                        callback.run();
                    }
                });
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public boolean isDraining() {
        return draining.get();
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE + 1;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
