package con.aidenjia.digitalhuman.conversation.api.rtc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 仅 RTC 进房凭据（无 ConvoAI agent）。
 *
 * @param appId 声网 App ID
 * @param channel 频道名
 * @param token 用户进房 Token；未开启鉴权时为 null
 * @param userId JWT sub
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RtcCredentialsResponse(
        @JsonProperty("appId") String appId, String channel, String token, String userId) {}
