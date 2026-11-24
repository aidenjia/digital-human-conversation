package con.aidenjia.digitalhuman.conversation.trtc.support;

import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import con.aidenjia.digitalhuman.conversation.exception.ConfigurationException;
import io.agora.media.RtcTokenBuilder2;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AgoraRtcTokenService {

    private final ConvoaiProperties properties;

    public AgoraRtcTokenService(ConvoaiProperties properties) {
        this.properties = properties;
    }

    public boolean isTokenAuthEnabled() {
        return properties.getRtc().isTokenAuthEnabled();
    }

    /** 为用户生成 RTC Token；未开启鉴权时返回 {@code null}。 */
    public String buildUserToken(String channel, String userId) {
        if (!isTokenAuthEnabled()) {
            return null;
        }
        validateCertificate();
        int expire = properties.getRtc().getTokenExpireSeconds();
        RtcTokenBuilder2 builder = new RtcTokenBuilder2();
        if (properties.isEnableStringUid()) {
            return builder.buildTokenWithUserAccount(
                    properties.getAppId(),
                    properties.getRtc().getAppCertificate(),
                    channel,
                    userId,
                    RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                    expire,
                    expire);
        }
        int uid = parseUid(userId, "client user id");
        return builder.buildTokenWithUid(
                properties.getAppId(),
                properties.getRtc().getAppCertificate(),
                channel,
                uid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                expire,
                expire);
    }

    /** 为智能体生成 RTC Token；未开启鉴权时返回 {@code null}。 */
    public String buildAgentToken(String channel) {
        if (!isTokenAuthEnabled()) {
            return null;
        }
        validateCertificate();
        int expire = properties.getRtc().getTokenExpireSeconds();
        RtcTokenBuilder2 builder = new RtcTokenBuilder2();
        if (properties.isEnableStringUid()) {
            return builder.buildTokenWithUserAccount(
                    properties.getAppId(),
                    properties.getRtc().getAppCertificate(),
                    channel,
                    properties.getAgentRtcUid(),
                    RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                    expire,
                    expire);
        }
        int agentUid = parseUid(properties.getAgentRtcUid(), "agent rtc userId");
        return builder.buildTokenWithUid(
                properties.getAppId(),
                properties.getRtc().getAppCertificate(),
                channel,
                agentUid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                expire,
                expire);
    }

    private void validateCertificate() {
        if (!StringUtils.hasText(properties.getRtc().getAppCertificate())) {
            throw new ConfigurationException("ConvoAI RTC app certificate is not configured");
        }
    }

    private static int parseUid(String value, String label) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0 || parsed > 0xFFFFFFFFL) {
                throw new NumberFormatException("out of uint32 range");
            }
            return (int) parsed;
        } catch (NumberFormatException ex) {
            throw new ConfigurationException(
                    "ConvoAI "
                            + label
                            + " must be a 32-bit unsigned integer when enable-string-userId is false");
        }
    }
}
