package con.aidenjia.digitalhuman.conversation.api.convoai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 声网 RTC 进房凭据：ConvoAI join 成功时 {@code ApiResponse.data} 载荷。
 *
 * @param appId 声网项目 App ID（JSON 字段名 {@code appId}）
 * @param channel RTC 频道名（服务端生成）
 * @param token 用户进房 Token；项目未开启 Token 鉴权时为 {@code null}
 * @param userId 客户端 RTC 用户 ID（JWT {@code sub}）
 * @param agentId 上游智能体 ID；客户端在 leave 时原样回传
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ConvoaiJoinCredentials(
        @JsonProperty("appId") String appId,
        String channel,
        String token,
        String userId,
        String agentId) {}
