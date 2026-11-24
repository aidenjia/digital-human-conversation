package con.aidenjia.digitalhuman.conversation.api.bailian.webrtc;

import con.aidenjia.digitalhuman.conversation.api.bailian.webrtc.dto.ExchangeBailianWebRtcRequest;
import con.aidenjia.digitalhuman.conversation.api.bailian.webrtc.dto.ExchangeBailianWebRtcResponse;
import con.aidenjia.digitalhuman.conversation.api.common.ApiResponse;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianWebRtcSignalingService;
import con.aidenjia.digitalhuman.conversation.security.CurrentUserIdAccessor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 百炼 WebRTC SDP 交换。不进声网，不接收人设与音色。 */
@RestController
@RequestMapping("/api/bailian/realtime/webrtc")
public class BailianRealtimeWebRtcController {

    private final BailianWebRtcSignalingService signalingService;
    private final CurrentUserIdAccessor currentUserIdAccessor;

    public BailianRealtimeWebRtcController(
            BailianWebRtcSignalingService signalingService,
            CurrentUserIdAccessor currentUserIdAccessor) {
        this.signalingService = signalingService;
        this.currentUserIdAccessor = currentUserIdAccessor;
    }

    @PostMapping
    public ApiResponse<ExchangeBailianWebRtcResponse> exchange(
            @Valid @RequestBody ExchangeBailianWebRtcRequest body) {
        return ApiResponse.ok(
                signalingService.exchange(
                        currentUserIdAccessor.requireUserId(), body.offerSdp(), body.model()));
    }
}
