package con.aidenjia.digitalhuman.conversation.avatar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import con.aidenjia.digitalhuman.conversation.infrastructure.avatar.AvatarDecisionClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Per-connection state: one asynchronous classification per response; no audio waits on it. */
public final class AvatarDecisionSession implements AutoCloseable {
    private final AvatarDecisionClient client;
    private final ObjectMapper mapper;
    private final Consumer<ObjectNode> send;
    private final LongSupplier clock;
    private final Map<String, Long> lastByIntent = new HashMap<>();
    private String userText = "";
    private String responseId = "";
    private String transcript = "";
    private String recentIntent = "none";
    private long lastIntentNanos = Long.MIN_VALUE;
    private boolean submitted;
    private boolean awaitingUserTranscript;
    private boolean openingReady;

    private record Offer(String intent, long expiresAt) {}

    private final Map<String, Offer> offers = new HashMap<>();
    private boolean blocked;
    private boolean suppressNext;
    private boolean closed;
    private CompletableFuture<AvatarDecision> pending;

    public AvatarDecisionSession(
            AvatarDecisionClient client, ObjectMapper mapper, Consumer<ObjectNode> send) {
        this(client, mapper, send, System::nanoTime);
    }

    AvatarDecisionSession(
            AvatarDecisionClient client,
            ObjectMapper mapper,
            Consumer<ObjectNode> send,
            LongSupplier clock) {
        this.client = client;
        this.mapper = mapper;
        this.send = send;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return client != null && client.isEnabled();
    }

    public synchronized void accept(JsonNode event) {
        if (!isEnabled() || closed) return;
        String type = event.path("type").asText();
        switch (type) {
            case "text.submit" -> {
                invalidate();
                userText = cap(event.path("text").asText(), 1000);
                awaitingUserTranscript = false;
                suppressNext = false;
            }
            case "input_audio_buffer.speech_started" -> {
                invalidate();
                userText = "";
                awaitingUserTranscript = true;
                suppressNext = false;
            }
            case "conversation.item.input_audio_transcription.delta" -> {
                userText =
                        cap(
                                userText + event.path("text").asText(event.path("delta").asText()),
                                1000);
                if (openingReady) submit();
            }
            case "conversation.item.input_audio_transcription.completed" -> {
                userText = cap(event.path("transcript").asText(userText), 1000);
                awaitingUserTranscript = false;
                if (openingReady) submit();
            }
            case "conversation.item.input_audio_transcription.failed" -> {
                awaitingUserTranscript = false;
                if (openingReady) submit();
            }
            case "avatar.gesture.started" -> {
                String intent = event.path("intent").asText();
                String cueId = event.path("cueId").asText();
                String motionId = event.path("motionId").asText();
                Offer offer = offers.get(cueId);
                long now = clock.getAsLong();
                if (offer != null
                        && offer.intent().equals(intent)
                        && responseId.equals(event.path("responseId").asText())
                        && motionId.matches("[a-z][a-z0-9_]{0,47}")
                        && now <= offer.expiresAt()) {
                    recentIntent = intent;
                    lastIntentNanos = now;
                    lastByIntent.put(intent, now);
                    offers.remove(cueId);
                }
            }
            case "response.created" -> {
                invalidate();
                responseId = event.path("response").path("id").asText("");
                transcript = "";
                submitted = false;
                openingReady = false;
                blocked = suppressNext;
                suppressNext = false;
            }
            case "response.audio_transcript.delta", "response.audio_transcript.done" -> {
                if (!responseId.equals(event.path("response_id").asText()) || responseId.isEmpty())
                    return;
                if ("response.audio_transcript.done".equals(type))
                    transcript = cap(event.path("transcript").asText(transcript), 160);
                else transcript = cap(transcript + event.path("delta").asText(), 160);
                if ("response.audio_transcript.done".equals(type)
                        || transcript.length() >= 100
                        || transcript.matches("(?s).*[。！？.!?\n].*")) {
                    openingReady = true;
                    submit();
                }
            }
            case "response.done" -> {
                if (!responseId.equals(event.path("response").path("id").asText())) return;
                if ("completed".equals(event.path("response").path("status").asText())) {
                    openingReady = true;
                    submit();
                } else invalidate();
            }
            default -> {}
        }
    }

    private static String cap(String value, int limit) {
        return value.substring(0, Math.min(limit, value.length()));
    }

    private void submit() {
        if (submitted || blocked || transcript.isBlank() || responseId.isBlank()) return;
        if (awaitingUserTranscript && userText.isBlank()) return;
        submitted = true;
        String id = responseId;
        long start = clock.getAsLong();
        String recent =
                lastIntentNanos != Long.MIN_VALUE && start - lastIntentNanos < 30_000_000_000L
                        ? recentIntent
                        : "none";
        pending = client.decide(new AvatarDecisionInput(userText, transcript, recent));
        pending.whenComplete((decision, error) -> complete(id, decision, error));
    }

    private synchronized void complete(String id, AvatarDecision decision, Throwable error) {
        if (closed || blocked || error != null || decision == null || !id.equals(responseId))
            return;
        String intent = decision.intent();
        long now = clock.getAsLong();
        if (lastIntentNanos != Long.MIN_VALUE
                && (now - lastIntentNanos < 8_000_000_000L
                        || (lastByIntent.containsKey(intent)
                                && now - lastByIntent.get(intent) < 30_000_000_000L)))
            intent = "none";
        if (!"none".equals(intent)) offers.put("opening", new Offer(intent, now + 1_500_000_000L));
        send.accept(
                mapper.createObjectNode()
                        .put("type", "avatar.decision")
                        .put("responseId", id)
                        .put("cueId", "opening")
                        .put("segmentId", "opening")
                        .put("intent", intent)
                        .put("emotion", decision.emotion())
                        .put("expiresInMs", 1500));
    }

    /** An explicit tool action wins over autonomous gestures, including its spoken confirmation. */
    public synchronized void suppressForExplicitAction() {
        blocked = true;
        suppressNext = true;
        invalidate();
    }

    private void invalidate() {
        responseId = "";
        openingReady = false;
        offers.clear();
        if (pending != null) {
            pending.cancel(true);
            pending = null;
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        invalidate();
    }
}
