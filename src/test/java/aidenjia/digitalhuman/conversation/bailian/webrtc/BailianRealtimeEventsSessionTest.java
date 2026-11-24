package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeEventCommandFactory;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** 控制连接把 session.update 交回客户端，并且不转发音频 delta。 */
@ExtendWith(MockitoExtension.class)
class BailianRealtimeEventsSessionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private WebSocketSession clientSession;
    @Mock private BailianRealtimeEventCommandFactory commandFactory;

    private List<String> clientMessages;
    private BailianRealtimeEventsSession session;

    @BeforeEach
    void setUp() throws Exception {
        clientMessages = new ArrayList<>();
        lenient().when(clientSession.getId()).thenReturn("events-1");
        lenient().when(clientSession.isOpen()).thenReturn(true);
        lenient()
                .doAnswer(
                        inv -> {
                            clientMessages.add(((TextMessage) inv.getArgument(0)).getPayload());
                            return null;
                        })
                .when(clientSession)
                .sendMessage(org.mockito.ArgumentMatchers.any(TextMessage.class));

        BailianRealtimeProperties properties = new BailianRealtimeProperties();
        properties.setGreetingEnabled(false);
        session =
                new BailianRealtimeEventsSession(
                        clientSession,
                        commandFactory,
                        properties,
                        objectMapper,
                        new BailianRealtimeEventsRegistry(),
                        false,
                        BailianRealtimeModels.PLUS,
                        "你叫黛黛",
                        "longanqian");
    }

    @Test
    @DisplayName("session.created 产出含握手人设与音色的 session.update")
    void sessionCreatedEmitsUpdate() throws Exception {
        when(commandFactory.sessionUpdateJson("你叫黛黛", "longanqian"))
                .thenReturn(
                        "{\"type\":\"session.update\",\"session\":{\"instructions\":\"## Role\\n你叫黛黛\",\"voice\":\"longanqian\"}}");

        session.handleClientText("{\"type\":\"session.created\"}");

        assertThat(clientMessages).hasSize(1);
        JsonNode root = objectMapper.readTree(clientMessages.get(0));
        assertThat(root.path("type").asText()).isEqualTo("downstream.command");
        assertThat(root.path("event").path("session").path("instructions").asText())
                .contains("你叫黛黛");
        assertThat(root.path("event").path("session").path("voice").asText())
                .isEqualTo("longanqian");
    }

    @Test
    void textSubmissionBeforeReadyIsRejected() throws Exception {
        session.handleClientText("{\"type\":\"text.submit\",\"id\":\"t1\",\"text\":\"你好\"}");
        assertThat(clientMessages).hasSize(1);
        assertThat(objectMapper.readTree(clientMessages.get(0)).path("type").asText())
                .isEqualTo("text.rejected");
    }

    @Test
    void providerUserTextDeltaIsForwardedToCaptions() throws Exception {
        session.handleClientText(
                "{\"type\":\"conversation.item.input_audio_transcription.delta\",\"text\":\"你好\",\"stash\":\"呀\"}");
        assertThat(clientMessages).hasSize(1);
        JsonNode caption = objectMapper.readTree(clientMessages.get(0));
        assertThat(caption.path("type").asText()).isEqualTo("transcript.user");
        assertThat(caption.path("text").asText()).isEqualTo("你好");
    }

    @Test
    @DisplayName("音频 delta 不产生 audio.delta")
    void audioDeltaIsDropped() {
        session.handleClientText("{\"type\":\"response.audio.delta\",\"delta\":\"AAAA\"}");
        assertThat(clientMessages).isEmpty();
    }

    @Test
    void obsoleteSingingToolCannotCancelPoetryOrInjectASingingConfirmation() throws Exception {
        var properties = new BailianRealtimeProperties();
        properties.setGreetingEnabled(false);
        var enabled =
                new BailianRealtimeEventsSession(
                        clientSession,
                        commandFactory,
                        properties,
                        objectMapper,
                        new BailianRealtimeEventsRegistry(),
                        true,
                        BailianRealtimeModels.FLASH,
                        "persona",
                        "voice");
        enabled.handleClientText(
                "{\"type\":\"conversation.item.input_audio_transcription.completed\",\"transcript\":\"念首古诗吧\"}");
        enabled.handleClientText("{\"type\":\"response.created\",\"response\":{\"id\":\"poem\"}}");
        enabled.handleClientText(
                "{\"type\":\"response.function_call_arguments.done\",\"name\":\"digital_human_action\",\"arguments\":\"{\\\"command\\\":\\\"sing\\\"}\",\"call_id\":\"poem-call\",\"response_id\":\"poem\"}");
        enabled.handleClientText(
                "{\"type\":\"response.audio_transcript.delta\",\"response_id\":\"poem\",\"delta\":\"春眠不觉晓，处处闻啼鸟。\"}");
        enabled.handleClientText(
                "{\"type\":\"response.done\",\"response\":{\"id\":\"poem\",\"status\":\"completed\"}}");
        var events = new ArrayList<JsonNode>();
        for (String message : clientMessages) events.add(objectMapper.readTree(message));
        assertThat(events).noneMatch(e -> "action".equals(e.path("type").asText()));
        assertThat(events)
                .anyMatch(
                        e ->
                                "transcript.assistant".equals(e.path("type").asText())
                                        && "春眠不觉晓，处处闻啼鸟。".equals(e.path("text").asText()));
        org.mockito.Mockito.verify(commandFactory, org.mockito.Mockito.never())
                .cancelResponseJson();
        org.mockito.Mockito.verify(commandFactory, org.mockito.Mockito.never())
                .actionResultJson(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("session.updated 回显票据模型与握手音色")
    void readyEchoesTicketModel() throws Exception {
        session.handleClientText("{\"type\":\"session.updated\"}");
        assertThat(clientMessages).hasSize(1);
        JsonNode ready = objectMapper.readTree(clientMessages.get(0));
        assertThat(ready.path("type").asText()).isEqualTo("session.ready");
        assertThat(ready.path("model").asText()).isEqualTo(BailianRealtimeModels.PLUS);
        assertThat(ready.path("voice").asText()).isEqualTo("longanqian");
    }

    @Test
    @DisplayName("Avatar lifecycle and assistant transcript preserve response identity")
    void avatarResponseLifecyclePreservesIdentity() throws Exception {

        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.created");
        event.putObject("response").put("id", "avatar-r1");
        session.handleClientText(event.toString());
        event = objectMapper.createObjectNode();
        event.put("type", "response.audio_transcript.delta");
        event.put("response_id", "avatar-r1");
        event.put("delta", "happy for you");
        session.handleClientText(event.toString());
        event = objectMapper.createObjectNode();
        event.put("type", "response.done");
        event.putObject("response").put("id", "avatar-r1").put("status", "cancelled");
        session.handleClientText(event.toString());
        List<JsonNode> events = new ArrayList<>();
        for (String message : clientMessages) {
            events.add(objectMapper.readTree(message));
        }
        assertThat(events)
                .anyMatch(
                        e ->
                                "avatar.response".equals(e.path("type").asText())
                                        && "created".equals(e.path("phase").asText())
                                        && "avatar-r1".equals(e.path("responseId").asText()));
        assertThat(events)
                .anyMatch(
                        e ->
                                "transcript.assistant".equals(e.path("type").asText())
                                        && "avatar-r1".equals(e.path("responseId").asText()));
        assertThat(events)
                .anyMatch(
                        e ->
                                "avatar.response".equals(e.path("type").asText())
                                        && "cancelled".equals(e.path("phase").asText())
                                        && "avatar-r1".equals(e.path("responseId").asText()));
    }
}
