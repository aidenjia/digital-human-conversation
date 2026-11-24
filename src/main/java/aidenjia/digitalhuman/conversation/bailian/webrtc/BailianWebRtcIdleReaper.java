package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

/** 扫描并关闭空闲的 WebRTC 控制会话。 */
@Component
@EnableScheduling
public class BailianWebRtcIdleReaper {

    private static final Logger log = LoggerFactory.getLogger(BailianWebRtcIdleReaper.class);

    private final BailianRealtimeEventsRegistry eventsRegistry;
    private final BailianRealtimeProperties properties;

    @Autowired
    public BailianWebRtcIdleReaper(
            BailianRealtimeEventsRegistry eventsRegistry, BailianRealtimeProperties properties) {
        this.eventsRegistry = eventsRegistry;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${bailian.realtime.heartbeat-interval-seconds:15}000")
    public void reapIdle() {
        long idleMs = Math.max(5, properties.getIdleTimeoutSeconds()) * 1000L;
        long now = System.currentTimeMillis();
        for (BailianRealtimeEventsSession session : eventsRegistry.all()) {
            if (now - session.lastClientActivityMs() > idleMs) {
                log.info(
                        "Bailian realtime events idle timeout sessionId={}",
                        session.clientSessionId());
                session.closeBoth(CloseStatus.SESSION_NOT_RELIABLE);
            }
        }
    }
}
