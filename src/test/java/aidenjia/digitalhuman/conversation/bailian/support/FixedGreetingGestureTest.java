package con.aidenjia.digitalhuman.conversation.bailian.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.avatar.AvatarDecision;
import con.aidenjia.digitalhuman.conversation.avatar.AvatarDecisionInput;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianRealtimeEventsRegistry;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianRealtimeEventsSession;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.infrastructure.avatar.AvatarDecisionClient;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeEventCommandFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class FixedGreetingGestureTest {
    final ObjectMapper mapper = new ObjectMapper();

    @Test
    void fixedGreetingDoesNotClassifyOrConsumeTheFirstUserGreeting() throws Exception {
        var socket = mock(WebSocketSession.class);
        when(socket.isOpen()).thenReturn(true);
        var messages = new ArrayList<JsonNode>();
        doAnswer(
                        inv -> {
                            messages.add(
                                    mapper.readTree(
                                            ((TextMessage) inv.getArgument(0)).getPayload()));
                            return null;
                        })
                .when(socket)
                .sendMessage(any(TextMessage.class));
        var upstream = mock(BailianRealtimeEventCommandFactory.class);
        var properties = new BailianRealtimeProperties();
        properties.setGreetingEnabled(true);
        properties.setGreetingText("你好，有什么想聊的吗");
        var inputs = new ArrayList<AvatarDecisionInput>();
        AvatarDecisionClient classifier =
                new AvatarDecisionClient() {
                    public boolean isEnabled() {
                        return true;
                    }

                    public CompletableFuture<AvatarDecision> decide(AvatarDecisionInput input) {
                        inputs.add(input);
                        return CompletableFuture.completedFuture(
                                new AvatarDecision("greet", "happy"));
                    }
                };
        Consumer<String> client;
        Consumer<JsonNode> provider;
        when(upstream.greetingJson(anyString()))
                .thenReturn(List.of("{\"type\":\"response.create\"}"));
        var session =
                new BailianRealtimeEventsSession(
                        socket,
                        upstream,
                        properties,
                        mapper,
                        new BailianRealtimeEventsRegistry(),
                        true,
                        BailianRealtimeModels.FLASH,
                        "你叫黛黛",
                        "longanqian",
                        classifier);
        client = session::handleClientText;
        provider = event -> session.handleClientText(event.toString());

        provider.accept(mapper.createObjectNode().put("type", "session.updated"));
        response(provider, "response.created", "welcome");
        transcript(provider, "welcome", "你好，有什么想聊的吗？");
        response(provider, "response.done", "welcome");
        assertTrue(inputs.isEmpty(), "固定欢迎语不应调用动作分类模型");
        assertTrue(
                messages.stream()
                        .noneMatch(e -> e.path("type").asText().equals("avatar.decision")));
        assertTrue(
                messages.stream()
                        .anyMatch(e -> e.path("type").asText().equals("transcript.assistant")),
                "欢迎语字幕必须保留");

        client.accept("{\"type\":\"text.submit\",\"id\":\"t1\",\"text\":\"你好\"}");
        response(provider, "response.created", "user-first");
        transcript(provider, "user-first", "哼，终于来啦？");
        response(provider, "response.done", "user-first");
        assertEquals(1, inputs.size());
        assertEquals("你好", inputs.get(0).userText());
        assertEquals("none", inputs.get(0).recentIntent());
        assertEquals("greet", decision(messages, "user-first").path("intent").asText());

        client.accept(
                "{\"type\":\"avatar.gesture.started\",\"responseId\":\"user-first\",\"intent\":\"greet\",\"cueId\":\"opening\",\"motionId\":\"wave\"}");
        client.accept("{\"type\":\"text.submit\",\"id\":\"t2\",\"text\":\"你好\"}");
        response(provider, "response.created", "user-repeat");
        transcript(provider, "user-repeat", "又来啦？");
        assertEquals("greet", inputs.get(1).recentIntent());
        assertEquals(
                "none",
                decision(messages, "user-repeat").path("intent").asText(),
                "真实用户动作开始后仍保留原冷却规则");
    }

    private JsonNode decision(List<JsonNode> messages, String id) {
        return messages.stream()
                .filter(
                        e ->
                                "avatar.decision".equals(e.path("type").asText())
                                        && id.equals(e.path("responseId").asText()))
                .findFirst()
                .orElseThrow();
    }

    private void response(Consumer<JsonNode> provider, String type, String id) {
        var event = mapper.createObjectNode().put("type", type);
        event.putObject("response").put("id", id).put("status", "completed");
        provider.accept(event);
    }

    private void transcript(Consumer<JsonNode> provider, String id, String text) {
        provider.accept(
                mapper.createObjectNode()
                        .put("type", "response.audio_transcript.delta")
                        .put("response_id", id)
                        .put("delta", text));
    }
}
