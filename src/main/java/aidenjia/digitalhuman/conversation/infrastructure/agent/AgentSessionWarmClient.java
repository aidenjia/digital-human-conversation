package con.aidenjia.digitalhuman.conversation.infrastructure.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
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
 * digital-human-agent {@code POST /v1/sessions/warm} 客户端。
 *
 * <p>鉴权复用 {@code convoai.llm.api-key}；URL 优先 {@code convoai.session-warm.url}，否则从 llm.url 推导。使用共享
 * OkHttp 连接池；场景超时通过 {@code newBuilder().callTimeout} 覆盖。失败只打 WARN，不抛异常。
 */
@Component
public class AgentSessionWarmClient {

    private static final Logger log = LoggerFactory.getLogger(AgentSessionWarmClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String CHAT_COMPLETIONS_SUFFIX = "/v1/chat/completions";
    private static final String WARM_SUFFIX = "/v1/sessions/warm";
    private static final int MESSAGE_TRUNCATE = 200;

    private final OkHttpClient httpClient;
    private final ConvoaiProperties properties;
    private final ObjectMapper objectMapper;

    public AgentSessionWarmClient(
            OkHttpClient httpClient, ConvoaiProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步调用 session warm。
     *
     * @return {@code true} 当 agent 返回 HTTP 2xx 且 {@code code == 0}
     */
    public boolean warm(
            String clientUserId,
            String conversationId,
            String deviceId,
            String digitalHumanType,
            String timezone,
            String replyLanguage) {
        ConvoaiProperties.SessionWarm warmCfg = properties.getSessionWarm();
        String url = resolveWarmUrl(warmCfg.getUrl(), properties.getLlmUrl());
        String apiKey = properties.getLlmApiKey();
        if (url == null) {
            log.warn("session warm skipped reason=unresolved-url");
            return false;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("session warm skipped reason=missing-api-key");
            return false;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientUserId", clientUserId);
        body.put("conversationId", conversationId);
        body.put("deviceId", deviceId);
        body.put("digitalHumanType", digitalHumanType);
        if (timezone != null && !timezone.isBlank()) {
            body.put("timezone", timezone.trim());
        }
        if (replyLanguage != null && !replyLanguage.isBlank()) {
            body.put("replyLanguage", replyLanguage.trim());
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            log.warn("session warm failed reason=serialize", e);
            return false;
        }

        long timeoutMs = warmCfg.getTimeoutMs() > 0 ? warmCfg.getTimeoutMs() : 5000L;
        // newBuilder 共享连接池与 Dispatcher，仅覆盖 callTimeout
        OkHttpClient callClient =
                httpClient.newBuilder().callTimeout(Duration.ofMillis(timeoutMs)).build();

        Request request =
                new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(json, JSON))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .build();

        long started = System.currentTimeMillis();
        log.info("session warm requested");
        try (Response response = callClient.newCall(request).execute()) {
            long durationMs = System.currentTimeMillis() - started;
            String responseBody = response.body() != null ? response.body().string() : "";
            int httpStatus = response.code();
            if (!response.isSuccessful()) {
                log.warn(
                        "session warm failed httpStatus={} durationMs={} body={}",
                        httpStatus,
                        durationMs,
                        truncate(responseBody));
                return false;
            }
            Integer code = readCode(responseBody);
            if (code != null && code == 0) {
                log.info("session warm succeeded durationMs={}", durationMs);
                return true;
            }
            String message = readMessage(responseBody);
            log.warn(
                    "session warm failed httpStatus={} code={} durationMs={} message={}",
                    httpStatus,
                    code,
                    durationMs,
                    truncate(message));
            return false;
        } catch (IOException e) {
            long durationMs = System.currentTimeMillis() - started;
            log.warn("session warm failed reason=network durationMs={}", durationMs, e);
            return false;
        }
    }

    /** package-visible for unit tests. */
    static String resolveWarmUrl(String explicitUrl, String llmUrl) {
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return explicitUrl.trim();
        }
        if (llmUrl == null || llmUrl.isBlank()) {
            return null;
        }
        String trimmed = llmUrl.trim();
        int idx = trimmed.indexOf(CHAT_COMPLETIONS_SUFFIX);
        if (idx < 0) {
            return null;
        }
        return trimmed.substring(0, idx) + WARM_SUFFIX;
    }

    private Integer readCode(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode code = root.get("code");
            return code != null && code.isNumber() ? code.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String readMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode message = root.get("message");
            return message != null && !message.isNull() ? message.asText() : responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= MESSAGE_TRUNCATE) {
            return value;
        }
        return value.substring(0, MESSAGE_TRUNCATE);
    }
}
