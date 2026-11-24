package con.aidenjia.digitalhuman.conversation.rtc;

import con.aidenjia.digitalhuman.conversation.api.rtc.dto.RtcCredentialsResponse;
import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import con.aidenjia.digitalhuman.conversation.security.CurrentUserIdAccessor;
import con.aidenjia.digitalhuman.conversation.trtc.RoomIdGenerator;
import con.aidenjia.digitalhuman.conversation.trtc.support.AgoraRtcTokenService;
import org.springframework.stereotype.Service;

/** 签发声网 RTC 用户进房凭据；不创建 ConvoAI 智能体。 */
@Service
public class RtcCredentialsService {

    private final ConvoaiProperties convoaiProperties;
    private final AgoraRtcTokenService rtcTokenService;
    private final CurrentUserIdAccessor currentUserIdAccessor;

    public RtcCredentialsService(
            ConvoaiProperties convoaiProperties,
            AgoraRtcTokenService rtcTokenService,
            CurrentUserIdAccessor currentUserIdAccessor) {
        this.convoaiProperties = convoaiProperties;
        this.rtcTokenService = rtcTokenService;
        this.currentUserIdAccessor = currentUserIdAccessor;
    }

    public RtcCredentialsResponse issue() {
        String userId = currentUserIdAccessor.requireUserId();
        String channel = RoomIdGenerator.generate();
        String token = rtcTokenService.buildUserToken(channel, userId);
        return new RtcCredentialsResponse(convoaiProperties.getAppId(), channel, token, userId);
    }
}
