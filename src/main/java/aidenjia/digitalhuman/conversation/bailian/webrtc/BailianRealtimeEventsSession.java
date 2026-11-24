package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import con.aidenjia.digitalhuman.conversation.avatar.AvatarDecisionSession;
import con.aidenjia.digitalhuman.conversation.bailian.action.RealtimeActionCatalog;
import con.aidenjia.digitalhuman.conversation.bailian.support.AvatarResponseEvents;
import con.aidenjia.digitalhuman.conversation.bailian.support.RealtimeTextTurns;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.infrastructure.avatar.AvatarDecisionClient;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeEventCommandFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** WebRTC 控制会话：把百炼事件写成 {@code downstream.command} 交给客户端的 DataChannel。 */
public final class BailianRealtimeEventsSession {

    private static final Logger log = LoggerFactory.getLogger(BailianRealtimeEventsSession.class);

    private final WebSocketSession clientSession;
    private final BailianRealtimeEventCommandFactory commandFactory;
    private final BailianRealtimeProperties properties;
    private final ObjectMapper objectMapper;
    private final BailianRealtimeEventsRegistry registry;
    private final boolean enableAction;
    private final AvatarDecisionSession avatarDecisions;
    private final RealtimeTextTurns textTurns;
    private final String model;
    private final String persona;
    private final String voice;

    private final AtomicLong lastClientActivityMs = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean sessionUpdated = new AtomicBoolean(false);
    private final AtomicBoolean greetingSent = new AtomicBoolean(false);
    private final AtomicBoolean awaitingGreetingDone = new AtomicBoolean(false);
    private final AtomicBoolean actionToolsRegistered = new AtomicBoolean(false);
    private final AtomicBoolean actionThisResponse = new AtomicBoolean(false);
    private final AtomicBoolean cancelSent = new AtomicBoolean(false);
    private final List<String> responseMessageItemIds = new ArrayList<>();
    private volatile String pendingActionCallId;
    private volatile String pendingConfirmation;

    public BailianRealtimeEventsSession(
            WebSocketSession clientSession,
            BailianRealtimeEventCommandFactory commandFactory,
            BailianRealtimeProperties properties,
            ObjectMapper objectMapper,
            BailianRealtimeEventsRegistry registry,
            boolean enableAction,
            String model,
            String persona,
            String voice) {
        this(
                clientSession,
                commandFactory,
                properties,
                objectMapper,
                registry,
                enableAction,
                model,
                persona,
                voice,
                null);
    }

    public BailianRealtimeEventsSession(
            WebSocketSession clientSession,
            BailianRealtimeEventCommandFactory commandFactory,
            BailianRealtimeProperties properties,
            ObjectMapper objectMapper,
            BailianRealtimeEventsRegistry registry,
            boolean enableAction,
            String model,
            String persona,
            String voice,
            AvatarDecisionClient decisionClient) {
        this.clientSession = clientSession;
        this.commandFactory = commandFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.enableAction = enableAction;
        this.model = model;
        this.persona = persona;
        this.voice = voice;
        this.avatarDecisions =
                new AvatarDecisionSession(
                        enableAction ? decisionClient : null, objectMapper, this::sendClientJson);
        this.textTurns =
                new RealtimeTextTurns(
                        objectMapper,
                        this::emit,
                        this::sendClientJson,
                        text -> {
                            actionThisResponse.set(false);
                            pendingConfirmation = null;
                            pendingActionCallId = null;
                            awaitingGreetingDone.set(false);
                            cancelSent.set(false);
                            registerActionToolsIfNeeded();
                            avatarDecisions.accept(
                                    objectMapper
                                            .createObjectNode()
                                            .put("type", "text.submit")
                                            .put("text", text));
                        });
    }

    public String clientSessionId() {
        return clientSession.getId();
    }

    public long lastClientActivityMs() {
        return lastClientActivityMs.get();
    }

    public void handleClientText(String payload) {
        lastClientActivityMs.set(System.currentTimeMillis());
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText("");
            switch (type) {
                case "text.submit" -> textTurns.submit(root, sessionUpdated.get() && !closed.get());
                case "ping" -> sendClientJson(objectMapper.createObjectNode().put("type", "pong"));
                case "audio.append", "response.audio.delta" -> {
                    // 音频留在 RTP / DataChannel，控制连接不转发
                }
                default -> handleBailianEvent(root);
            }
        } catch (Exception ex) {
            log.warn("events client message handle failed sessionId={}", clientSessionId());
            sendClientError("invalid client message");
        }
    }

    public void sendDraining() {
        sendClientJson(objectMapper.createObjectNode().put("type", "server.draining"));
    }

    public void closeBoth(CloseStatus status) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        avatarDecisions.close();
        registry.remove(clientSessionId());
        if (clientSession.isOpen()) {
            try {
                clientSession.close(status);
            } catch (IOException ex) {
                log.warn("close events ws failed sessionId={}", clientSessionId());
            }
        }
    }

    private void handleBailianEvent(JsonNode event) {
        if (!textTurns.accept(event)) return;
        String type = event.path("type").asText("");
        // Fixed welcome speech is not a user exchange and must not consume gesture cooldown.
        // Keep input/transcription events flowing if the user interrupts the welcome.
        if (!awaitingGreetingDone.get() || !type.startsWith("response.")) {
            avatarDecisions.accept(event);
        }
        switch (type) {
            case "response.created" -> {
                ObjectNode lifecycle = AvatarResponseEvents.lifecycle(event, objectMapper);
                if (lifecycle != null) {
                    lifecycle.put("decisionMode", avatarDecisions.isEnabled() ? "model" : "local");
                    sendClientJson(lifecycle);
                }
            }
            case "session.created" -> emit(commandFactory.sessionUpdateJson(persona, voice));
            case "session.updated" -> {
                if (!sessionUpdated.compareAndSet(false, true)) {
                    return;
                }
                ObjectNode ready = objectMapper.createObjectNode();
                ready.put("type", "session.ready");
                ready.put("model", model);
                ready.put("voice", voice);
                ready.put("enableAction", enableAction);
                sendClientJson(ready);
                if (!maybeSpeakGreeting()) {
                    registerActionToolsIfNeeded();
                }
            }
            case "input_audio_buffer.speech_started" ->
                    sendClientJson(objectMapper.createObjectNode().put("type", "speech.started"));
            case "input_audio_buffer.speech_stopped" -> {
                ObjectNode out = objectMapper.createObjectNode();
                out.put("type", "speech.stopped");
                String reason = text(event, "reason");
                if (reason != null) {
                    out.put("reason", reason);
                }
                sendClientJson(out);
            }
            case "conversation.item.input_audio_transcription.delta" -> {
                String text = text(event, "text");
                if (text == null) text = text(event, "delta");
                if (text != null) {
                    ObjectNode out = objectMapper.createObjectNode();
                    out.put("type", "transcript.user");
                    out.put("text", text);
                    sendClientJson(out);
                }
            }
            case "response.audio_transcript.delta" -> {
                String text = text(event, "delta");
                if (text != null) {
                    ObjectNode out = objectMapper.createObjectNode();
                    out.put("type", "transcript.assistant");
                    out.put("responseId", event.path("response_id").asText(""));
                    out.put("text", text);
                    sendClientJson(out);
                }
            }
            case "error" -> {
                JsonNode err = event.path("error");
                String message = err.path("message").asText("upstream error");
                String code = err.path("code").asText("");
                if ("invalid_value".equals(code) && message.contains("no active response")) {
                    return;
                }
                if (awaitingGreetingDone.compareAndSet(true, false)) {
                    registerActionToolsIfNeeded();
                }
                sendClientError(message);
            }
            case "response.output_item.done" -> rememberAssistantMessage(event.path("item"));
            case "response.function_call_arguments.done" -> handleActionFunctionCall(event);
            case "response.done" -> {
                ObjectNode lifecycle = AvatarResponseEvents.lifecycle(event, objectMapper);
                if (lifecycle != null) {
                    lifecycle.put("decisionMode", avatarDecisions.isEnabled() ? "model" : "local");
                    sendClientJson(lifecycle);
                }
                finishActionResponse();
                if (awaitingGreetingDone.compareAndSet(true, false)) {
                    registerActionToolsIfNeeded();
                }
            }
            default -> {
                // ignore
            }
        }
    }

    private void handleActionFunctionCall(JsonNode event) {
        if (!enableAction || awaitingGreetingDone.get()) {
            return;
        }
        if (!RealtimeActionCatalog.TOOL_NAME.equals(event.path("name").asText(""))) {
            return;
        }
        String command = commandFromArguments(event.path("arguments").asText(""));
        var action = RealtimeActionCatalog.fromCommand(command);
        if (action.isEmpty()) {
            return;
        }
        avatarDecisions.suppressForExplicitAction();
        actionThisResponse.set(true);
        pendingActionCallId = event.path("call_id").asText("");
        pendingConfirmation = action.get().confirmation();
        ObjectNode out = objectMapper.createObjectNode();
        out.put("type", "action");
        out.put("command", action.get().command());
        out.put("displayName", action.get().displayName());
        out.put("confirmation", action.get().confirmation());
        sendClientJson(out);
        cancelOnce();
        deleteRememberedMessages();
    }

    private void rememberAssistantMessage(JsonNode item) {
        if (item == null || !"message".equals(item.path("type").asText(""))) {
            return;
        }
        String id = text(item, "id");
        if (id == null) {
            return;
        }
        synchronized (responseMessageItemIds) {
            if (!responseMessageItemIds.contains(id)) {
                responseMessageItemIds.add(id);
            }
        }
        if (actionThisResponse.get()) {
            emit(commandFactory.deleteItemJson(id));
        }
    }

    private void finishActionResponse() {
        boolean speak = actionThisResponse.get() && pendingConfirmation != null;
        String callId = pendingActionCallId;
        String confirmation = pendingConfirmation;
        if (actionThisResponse.get()) {
            deleteRememberedMessages();
        }
        actionThisResponse.set(false);
        cancelSent.set(false);
        pendingActionCallId = null;
        pendingConfirmation = null;
        synchronized (responseMessageItemIds) {
            responseMessageItemIds.clear();
        }
        if (speak && confirmation != null) {
            emitAll(commandFactory.actionResultJson(callId, confirmation));
        }
    }

    private void deleteRememberedMessages() {
        List<String> ids;
        synchronized (responseMessageItemIds) {
            ids = List.copyOf(responseMessageItemIds);
            responseMessageItemIds.clear();
        }
        for (String id : ids) {
            emit(commandFactory.deleteItemJson(id));
        }
    }

    private boolean maybeSpeakGreeting() {
        if (!properties.isGreetingEnabled()) {
            return false;
        }
        String text = properties.getGreetingText();
        if (text == null || text.isBlank() || !greetingSent.compareAndSet(false, true)) {
            return false;
        }
        awaitingGreetingDone.set(true);
        emitAll(commandFactory.greetingJson(text));
        return true;
    }

    private void registerActionToolsIfNeeded() {
        if (!enableAction || !actionToolsRegistered.compareAndSet(false, true)) {
            return;
        }
        emit(commandFactory.actionToolsUpdateJson(persona));
    }

    private void cancelOnce() {
        if (cancelSent.compareAndSet(false, true)) {
            emit(commandFactory.cancelResponseJson());
        }
    }

    private void emitAll(List<String> jsonEvents) {
        for (String json : jsonEvents) {
            emit(json);
        }
    }

    private void emit(String bailianEventJson) {
        if (bailianEventJson == null || bailianEventJson.isBlank()) {
            return;
        }
        try {
            if ("response.create"
                    .equals(objectMapper.readTree(bailianEventJson).path("type").asText()))
                textTurns.requested();
            ObjectNode wrap = objectMapper.createObjectNode();
            wrap.put("type", "downstream.command");
            wrap.set("event", objectMapper.readTree(bailianEventJson));
            sendClientJson(wrap);
        } catch (Exception ex) {
            log.warn("events command encode failed sessionId={}", clientSessionId());
        }
    }

    private String commandFromArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return "";
        }
        try {
            return objectMapper.readTree(arguments).path("command").asText("");
        } catch (Exception ex) {
            return "";
        }
    }

    private void sendClientError(String message) {
        ObjectNode out = objectMapper.createObjectNode();
        out.put("type", "error");
        out.put("message", message);
        sendClientJson(out);
    }

    private void sendClientJson(ObjectNode node) {
        if (!clientSession.isOpen() || closed.get()) {
            return;
        }
        try {
            synchronized (clientSession) {
                clientSession.sendMessage(new TextMessage(node.toString()));
            }
        } catch (IOException ex) {
            log.warn("send events client failed sessionId={}", clientSessionId());
            closeBoth(CloseStatus.SERVER_ERROR);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        String v = n.asText();
        return v == null || v.isEmpty() ? null : v;
    }
}
