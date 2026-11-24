package con.aidenjia.digitalhuman.conversation.infrastructure.convoai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import con.aidenjia.digitalhuman.conversation.exception.ConfigurationException;
import con.aidenjia.digitalhuman.conversation.exception.UpstreamException;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 声网对话式 AI 引擎 REST 客户端（OkHttp）：join / leave 共用共享连接池。
 *
 * <p>join 直连 REST 以支持 v2.6 {@code turn_detection} / {@code interruption}；leave 为 {@code POST
 * .../agents/{agentId}/leave}。
 */
@Component
public class AgoraRestClient {

    private static final Logger log = LoggerFactory.getLogger(AgoraRestClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String BASE_URL = "https://api.agora.io";
    private static final int ERROR_BODY_TRUNCATE = 500;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConvoaiProperties properties;

    public AgoraRestClient(
            OkHttpClient httpClient, ConvoaiProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 POST /join 创建对话式智能体并加入 RTC 频道。
     *
     * @param body 请求体（name + properties）
     * @return 创建结果（agentId、createTs、status）
     */
    public JoinResponse join(Map<String, Object> body) {
        String url = buildJoinUrl();
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new UpstreamException("Failed to serialize join request body", e);
        }
        log.debug("ConvoAI REST join request url={} body={}", url, json);

        Request request =
                new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(json, JSON))
                        .header("Authorization", buildAuthHeader())
                        .header("Content-Type", "application/json")
                        .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            int code = response.code();
            if (!response.isSuccessful()) {
                log.warn(
                        "ConvoAI REST join failed httpStatus={} body={}",
                        code,
                        truncate(responseBody));
                throw new UpstreamException(
                        "Agora ConvoAI join returned HTTP " + code + ": " + truncate(responseBody));
            }
            JoinResponse result = objectMapper.readValue(responseBody, JoinResponse.class);
            log.info(
                    "ConvoAI REST join succeeded agentId={} status={}",
                    result.agentId(),
                    result.status());
            return result;
        } catch (IOException e) {
            throw new UpstreamException("Agora ConvoAI join network error", e);
        }
    }

    /**
     * 调用 POST .../agents/{agentId}/leave 停止智能体。
     *
     * @param agentId 智能体实例 ID
     */
    public void leave(String agentId) {
        String url = buildLeaveUrl(agentId);
        Request request =
                new Request.Builder()
                        .url(url)
                        .post(RequestBody.create("", JSON))
                        .header("Authorization", buildAuthHeader())
                        .header("Content-Type", "application/json")
                        .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            int code = response.code();
            if (!response.isSuccessful()) {
                log.warn(
                        "ConvoAI REST leave failed httpStatus={} agentId={} body={}",
                        code,
                        agentId,
                        truncate(responseBody));
                throw new UpstreamException(
                        "Agora ConvoAI leave returned HTTP "
                                + code
                                + ": "
                                + truncate(responseBody));
            }
            log.info("ConvoAI REST leave succeeded agentId={}", agentId);
        } catch (IOException e) {
            throw new UpstreamException("Agora ConvoAI leave network error", e);
        }
    }

    private String buildJoinUrl() {
        return BASE_URL
                + "/"
                + regionPrefix()
                + "/api/conversational-ai-agent/v2/projects/"
                + properties.getAppId()
                + "/join";
    }

    private String buildLeaveUrl(String agentId) {
        return BASE_URL
                + "/"
                + regionPrefix()
                + "/api/conversational-ai-agent/v2/projects/"
                + properties.getAppId()
                + "/agents/"
                + agentId
                + "/leave";
    }

    private String regionPrefix() {
        return switch (properties.getServerRegion()) {
            case "CHINESE_MAINLAND" -> "cn";
            case "GLOBAL" -> "api";
            default ->
                    throw new ConfigurationException(
                            "Unsupported ConvoAI server region: " + properties.getServerRegion());
        };
    }

    private String buildAuthHeader() {
        String credentials = properties.getUsername() + ":" + properties.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= ERROR_BODY_TRUNCATE) {
            return value;
        }
        return value.substring(0, ERROR_BODY_TRUNCATE);
    }

    public record JoinResponse(
            @JsonProperty("agent_id") String agentId,
            @JsonProperty("create_ts") long createTs,
            String status) {}
}
