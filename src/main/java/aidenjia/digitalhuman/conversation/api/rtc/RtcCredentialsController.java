package con.aidenjia.digitalhuman.conversation.api.rtc;

import con.aidenjia.digitalhuman.conversation.api.common.ApiResponse;
import con.aidenjia.digitalhuman.conversation.api.rtc.dto.RtcCredentialsResponse;
import con.aidenjia.digitalhuman.conversation.rtc.RtcCredentialsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 签发声网 RTC 进房凭据，不创建 ConvoAI agent。
 *
 * <p>鉴权：{@code Authorization: Bearer <Users access token>}；不创建 ConvoAI agent。
 */
@RestController
@RequestMapping("/api/rtc/credentials")
public class RtcCredentialsController {

    private final RtcCredentialsService rtcCredentialsService;

    public RtcCredentialsController(RtcCredentialsService rtcCredentialsService) {
        this.rtcCredentialsService = rtcCredentialsService;
    }

    @PostMapping
    public ApiResponse<RtcCredentialsResponse> issue() {
        return ApiResponse.ok(rtcCredentialsService.issue());
    }
}
