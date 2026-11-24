package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** 进程内 WebRTC 控制会话（心跳与 drain）。不持有百炼上游 WebSocket。 */
@Component
public class BailianRealtimeEventsRegistry {

    private final ConcurrentHashMap<String, BailianRealtimeEventsSession> byClientSessionId =
            new ConcurrentHashMap<>();

    public void register(BailianRealtimeEventsSession session) {
        byClientSessionId.put(session.clientSessionId(), session);
    }

    public void remove(String clientSessionId) {
        byClientSessionId.remove(clientSessionId);
    }

    public BailianRealtimeEventsSession get(String clientSessionId) {
        return byClientSessionId.get(clientSessionId);
    }

    public Collection<BailianRealtimeEventsSession> all() {
        return byClientSessionId.values();
    }
}
