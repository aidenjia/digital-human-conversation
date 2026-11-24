package con.aidenjia.digitalhuman.conversation.infrastructure.avatar;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.avatar.AvatarDecision;
import con.aidenjia.digitalhuman.conversation.avatar.AvatarDecisionInput;
import con.aidenjia.digitalhuman.conversation.config.AvatarDecisionProperties;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;

class QwenAvatarDecisionClientTest {
    final ObjectMapper mapper = new ObjectMapper();
    final AvatarDecisionProperties props = new AvatarDecisionProperties();
    final BailianRealtimeProperties realtime = new BailianRealtimeProperties();
    final AtomicReference<Request> request = new AtomicReference<>();

    QwenAvatarDecisionClient client(String reply, int status) {
        props.setApiKey("test-key");
        props.setTimeoutMs(2000);
        var shared =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                chain -> {
                                    request.set(chain.request());
                                    return new Response.Builder()
                                            .request(chain.request())
                                            .protocol(Protocol.HTTP_1_1)
                                            .code(status)
                                            .message("test")
                                            .body(
                                                    ResponseBody.create(
                                                            reply,
                                                            MediaType.get("application/json")))
                                            .build();
                                })
                        .build();
        return new QwenAvatarDecisionClient(props, realtime, mapper, shared);
    }

    String response(String content, String finish) throws Exception {
        var root = mapper.createObjectNode();
        var choice = root.putArray("choices").addObject().put("finish_reason", finish);
        choice.putObject("message").put("content", content);
        return root.toString();
    }

    final AvatarDecisionInput input = new AvatarDecisionInput("hello", "Hello!", "none");

    @Test
    void sendsNativeStrictSchemaAndUsesConfiguredModelWithoutThinking() throws Exception {
        var c = client(response("{\"intent\":\"greet\",\"emotion\":\"happy\"}", "stop"), 200);
        assertEquals(
                new AvatarDecision("greet", "happy"), c.decide(input).get(3, TimeUnit.SECONDS));
        assertEquals("Bearer test-key", request.get().header("Authorization"));
        assertEquals(
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                request.get().url().toString());
        var buffer = new Buffer();
        request.get().body().writeTo(buffer);
        var body = mapper.readTree(buffer.readUtf8());
        assertEquals("qwen3.7-flash", body.path("model").asText());
        assertFalse(body.path("enable_thinking").asBoolean(true));
        assertFalse(body.path("stream").asBoolean(true));
        var format = body.path("response_format");
        assertEquals("json_schema", format.path("type").asText());
        assertTrue(format.path("json_schema").path("strict").asBoolean());
        assertFalse(
                format.path("json_schema")
                        .path("schema")
                        .path("additionalProperties")
                        .asBoolean(true));
        assertEquals(2, format.path("json_schema").path("schema").path("required").size());
    }

    @Test
    void schemaFailureAndTruncationDoNotFallBackToUnconstrainedText() throws Exception {
        for (String content :
                new String[] {"{\"intent\":\"fly\",\"emotion\":\"happy\"}", "markdown"}) {
            var c = client(response(content, "stop"), 200);
            assertThrows(Exception.class, () -> c.decide(input).get(3, TimeUnit.SECONDS));
        }
        var c = client(response("{\"intent\":\"none\",\"emotion\":\"neutral\"}", "length"), 200);
        assertThrows(Exception.class, () -> c.decide(input).get(3, TimeUnit.SECONDS));
    }

    @Test
    void explanationIntentUsesTheSameStrictSingleRequestContract() throws Exception {
        var c = client(response("{\"intent\":\"explain\",\"emotion\":\"neutral\"}", "stop"), 200);
        var decision =
                c.decide(new AvatarDecisionInput("为什么天空是蓝色", "因为空气会散射太阳光。", "none"))
                        .get(3, TimeUnit.SECONDS);
        assertEquals("explain", decision.intent());
        var buffer = new Buffer();
        request.get().body().writeTo(buffer);
        var body = mapper.readTree(buffer.readUtf8());
        assertTrue(
                body.path("response_format")
                        .path("json_schema")
                        .path("schema")
                        .path("properties")
                        .path("intent")
                        .path("enum")
                        .toString()
                        .contains("\"explain\""));
        assertEquals(2, body.path("messages").size());
        assertFalse(body.path("stream").asBoolean(true));
    }

    @Test
    void httpErrorIsSanitizedAndNotRetried() {
        var c = client("secret provider error", 401);
        var error = assertThrows(Exception.class, () -> c.decide(input).get(3, TimeUnit.SECONDS));
        assertFalse(error.toString().contains("secret provider error"));
    }

    @Test
    void missingKeyOrDisabledFeatureDoesNotMakeHttpRequest() throws Exception {
        var c = client("", 200);
        props.setEnabled(false);
        assertEquals(AvatarDecision.NONE, c.decide(input).get());
        assertNull(request.get());
        var noKey =
                new QwenAvatarDecisionClient(
                        new AvatarDecisionProperties(), realtime, mapper, new OkHttpClient());
        assertFalse(noKey.isEnabled());
    }

    @Test
    void reusesRealtimeKeyAndWorkspaceWhenNoDecisionOverrideProvided() {
        realtime.setApiKey("existing-key");
        realtime.setWorkspaceId("workspace-test");
        var c = new QwenAvatarDecisionClient(props, realtime, mapper, new OkHttpClient());
        assertTrue(c.isEnabled());
    }

    @Test
    void deadlineCancelsNetworkWorkAndCannotBlockCaller() throws Exception {
        props.setApiKey("test-key");
        props.setTimeoutMs(100);
        var shared =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                chain -> {
                                    try {
                                        Thread.sleep(300);
                                    } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                    }
                                    throw new java.io.IOException("slow");
                                })
                        .build();
        var c = new QwenAvatarDecisionClient(props, realtime, mapper, shared);
        long start = System.nanoTime();
        var future = c.decide(input);
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 90);
        assertThrows(Exception.class, () -> future.get(1, TimeUnit.SECONDS));
    }
}
