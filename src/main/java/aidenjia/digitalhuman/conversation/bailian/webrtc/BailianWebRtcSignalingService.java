package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import con.aidenjia.digitalhuman.conversation.api.bailian.webrtc.dto.ExchangeBailianWebRtcResponse;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.exception.ApiException;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeWebRtcSignalingClient;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BailianWebRtcSignalingService {

    private final BailianRealtimeWebRtcSignalingClient signalingClient;
    private final BailianWebRtcTicketStore ticketStore;

    public BailianWebRtcSignalingService(
            BailianRealtimeWebRtcSignalingClient signalingClient,
            BailianWebRtcTicketStore ticketStore) {
        this.signalingClient = signalingClient;
        this.ticketStore = ticketStore;
    }

    public ExchangeBailianWebRtcResponse exchange(String userId, String offerSdp, String model) {
        if (!StringUtils.hasText(offerSdp)) {
            throw new ApiException(400, "offerSdp is required");
        }
        String resolved = BailianRealtimeModels.resolve(model);
        if (!BailianRealtimeModels.isAllowed(resolved)) {
            throw new ApiException(400, "unsupported realtime model");
        }
        String answer = signalingClient.exchange(resolved, offerSdp);
        String relayId = UUID.randomUUID().toString().replace("-", "");
        ticketStore.save(relayId, userId, resolved);
        return new ExchangeBailianWebRtcResponse(answer, relayId, resolved);
    }
}
