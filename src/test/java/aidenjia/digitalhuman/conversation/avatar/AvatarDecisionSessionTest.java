package con.aidenjia.digitalhuman.conversation.avatar;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import con.aidenjia.digitalhuman.conversation.infrastructure.avatar.AvatarDecisionClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AvatarDecisionSessionTest {
    final ObjectMapper mapper = new ObjectMapper();
    final List<ObjectNode> events = new ArrayList<>();
    final List<AvatarDecisionInput> inputs = new ArrayList<>();
    CompletableFuture<AvatarDecision> reply = new CompletableFuture<>();
    final AvatarDecisionClient client =
            new AvatarDecisionClient() {
                public boolean isEnabled() {
                    return true;
                }

                public CompletableFuture<AvatarDecision> decide(AvatarDecisionInput input) {
                    inputs.add(input);
                    return reply;
                }
            };
    final AvatarDecisionSession session = new AvatarDecisionSession(client, mapper, events::add);

    void event(String json) throws Exception {
        session.accept(mapper.readTree(json));
    }

    void start(String id) throws Exception {
        var e = mapper.createObjectNode().put("type", "response.created");
        e.putObject("response").put("id", id);
        session.accept(e);
    }

    void text(String id, String delta) throws Exception {
        var e =
                mapper.createObjectNode()
                        .put("type", "response.audio_transcript.delta")
                        .put("response_id", id)
                        .put("delta", delta);
        session.accept(e);
    }

    @Test
    void directTextInvalidatesOldDecisionAndProvidesUserContext() throws Exception {
        start("old");
        text("old", "Welcome!");
        event("{\"type\":\"text.submit\",\"text\":\"你好\"}");
        assertTrue(reply.isCancelled());
        reply = new CompletableFuture<>();
        start("new");
        text("new", "哼，终于来啦！");
        assertEquals("你好", inputs.get(1).userText());
    }

    @Test
    void classifiesFirstSentenceOnceWithoutBlockingAndCarriesUserContext() throws Exception {
        event(
                "{\"type\":\"conversation.item.input_audio_transcription.completed\",\"transcript\":\"Hello\"}");
        start("r1");
        text("r1", "Hello");
        assertTrue(inputs.isEmpty());
        text("r1", " there!");
        text("r1", " Welcome again.");
        assertEquals(1, inputs.size());
        assertEquals("Hello", inputs.get(0).userText());
        assertEquals("Hello there!", inputs.get(0).assistantText());
        assertTrue(events.isEmpty());
        reply.complete(new AvatarDecision("greet", "happy"));
        assertEquals("r1", events.get(0).path("responseId").asText());
        assertEquals("greet", events.get(0).path("intent").asText());
    }

    @Test
    void providerTextDeltaReachesDecisionBeforeFinalTranscription() throws Exception {
        event("{\"type\":\"input_audio_buffer.speech_started\"}");
        event(
                "{\"type\":\"conversation.item.input_audio_transcription.delta\",\"text\":\"你好\",\"stash\":\"呀\"}");
        start("greeting");
        text("greeting", "哼，谁要跟你打招呼啦？");
        assertEquals(1, inputs.size());
        assertEquals("你好", inputs.get(0).userText());
    }

    @Test
    void completionFlushesShortReplyAndKeepsDecisionValidWhileAudioCanStillBeBuffered()
            throws Exception {
        start("r1");
        text("r1", "Yes");
        event("{\"type\":\"response.done\",\"response\":{\"id\":\"r1\",\"status\":\"completed\"}}");
        reply.complete(new AvatarDecision("agree", "neutral"));
        assertEquals(1, events.size());
    }

    @Test
    void bargeInCancelsInFlightAndDropsLateTranscripts() throws Exception {
        start("r1");
        text("r1", "Hello!");
        event("{\"type\":\"input_audio_buffer.speech_started\"}");
        assertTrue(reply.isCancelled());
        text("r1", "More!");
        assertTrue(events.isEmpty());
        assertEquals(1, inputs.size());
    }

    @Test
    void newResponseAndFailedResponseCancelPendingWork() throws Exception {
        start("r1");
        text("r1", "Hello!");
        start("r2");
        assertTrue(reply.isCancelled());
        reply = new CompletableFuture<>();
        text("r2", "Yes!");
        event("{\"type\":\"response.done\",\"response\":{\"id\":\"r2\",\"status\":\"cancelled\"}}");
        assertTrue(reply.isCancelled());
        assertTrue(events.isEmpty());
    }

    @Test
    void explicitCommandSuppressesAutonomousGestureAndConfirmation() throws Exception {
        start("r1");
        text("r1", "Hello!");
        session.suppressForExplicitAction();
        assertTrue(reply.isCancelled());
        reply = new CompletableFuture<>();
        start("confirmation");
        text("confirmation", "I am waving!");
        assertEquals(1, inputs.size());
    }

    @Test
    void failuresAndClosedSessionsNeverEmitDecision() throws Exception {
        start("r1");
        text("r1", "Hello!");
        reply.completeExceptionally(new RuntimeException("timeout"));
        assertTrue(events.isEmpty());
        reply = new CompletableFuture<>();
        start("r2");
        text("r2", "Yes!");
        session.close();
        assertTrue(reply.isCancelled());
        assertTrue(events.isEmpty());
    }

    @Test
    void waitsForFinalUserTranscriptWhenReplyStartsFirst() throws Exception {
        event("{\"type\":\"input_audio_buffer.speech_started\"}");
        start("r1");
        text("r1", "哼，终于肯理我啦？");
        assertTrue(inputs.isEmpty());
        event(
                "{\"type\":\"conversation.item.input_audio_transcription.completed\",\"transcript\":\"你好。\"}");
        assertEquals(1, inputs.size());
        assertEquals("你好。", inputs.get(0).userText());
    }

    @Test
    void unplayedSuggestionDoesNotConsumeCooldown() throws Exception {
        start("welcome");
        text("welcome", "你好！");
        reply.complete(new AvatarDecision("greet", "happy"));
        reply = new CompletableFuture<>();
        start("greeting");
        text("greeting", "你好！");
        assertEquals("none", inputs.get(1).recentIntent());
        reply.complete(new AvatarDecision("greet", "happy"));
        assertEquals("greet", events.get(1).path("intent").asText());
    }

    @Test
    void repeatedGestureIsSuppressedAndRecentGesturePassedToModel() throws Exception {
        start("r1");
        text("r1", "Hello!");
        reply.complete(new AvatarDecision("greet", "happy"));
        event(
                "{\"type\":\"avatar.gesture.started\",\"responseId\":\"r1\",\"intent\":\"greet\",\"cueId\":\"opening\",\"motionId\":\"wave\"}");
        reply = new CompletableFuture<>();
        start("r2");
        text("r2", "Welcome!");
        assertEquals("greet", inputs.get(1).recentIntent());
        reply.complete(new AvatarDecision("greet", "happy"));
        assertEquals("none", events.get(1).path("intent").asText());
    }

    @Test
    void disabledClientDoesNotSubmit() throws Exception {
        try (var disabled = new AvatarDecisionSession(null, mapper, events::add)) {
            disabled.accept(
                    mapper.readTree("{\"type\":\"response.created\",\"response\":{\"id\":\"x\"}}"));
            assertFalse(disabled.isEnabled());
            assertTrue(inputs.isEmpty());
        }
    }

    @Test
    void cueIdentityAndMotionIdAreRequiredBeforeRecordingPlayback() throws Exception {
        start("r1");
        text("r1", "Hello!");
        reply.complete(new AvatarDecision("greet", "happy"));
        assertEquals("opening", events.get(0).path("cueId").asText());
        assertEquals("opening", events.get(0).path("segmentId").asText());
        assertFalse(events.get(0).has("gesture"));
        event(
                "{\"type\":\"avatar.gesture.started\",\"responseId\":\"r1\",\"cueId\":\"unknown\",\"intent\":\"greet\",\"motionId\":\"wave\"}");
        event(
                "{\"type\":\"avatar.gesture.started\",\"responseId\":\"r1\",\"cueId\":\"opening\",\"intent\":\"greet\",\"motionId\":\"../bad\"}");
        reply = new CompletableFuture<>();
        start("r2");
        text("r2", "Hello again!");
        assertEquals("none", inputs.get(1).recentIntent());
    }

    @Test
    void parserRejectsExtraFieldsDuplicatesUnknownEnumsAndTrailingContent() throws Exception {
        assertEquals(
                new AvatarDecision("greet", "happy"),
                AvatarDecision.parse("{\"intent\":\"greet\",\"emotion\":\"happy\"}", mapper));
        for (String json :
                List.of(
                        "{\"intent\":\"fly\",\"emotion\":\"happy\"}",
                        "{\"intent\":\"none\",\"emotion\":\"happy\",\"x\":1}",
                        "{\"intent\":\"none\",\"intent\":\"greet\",\"emotion\":\"happy\"}",
                        "{\"intent\":\"none\",\"emotion\":\"happy\"} {}")) {
            assertThrows(Exception.class, () -> AvatarDecision.parse(json, mapper));
        }
    }

    @Test
    void explanationIsAStrictSemanticIntentRatherThanAnAnimationField() throws Exception {
        assertEquals(
                "explain",
                AvatarDecision.parse("{\"intent\":\"explain\",\"emotion\":\"neutral\"}", mapper)
                        .intent());
        var intents = AvatarDecision.schema(mapper).path("properties").path("intent").path("enum");
        assertTrue(intents.toString().contains("\"explain\""));
        assertThrows(
                Exception.class,
                () ->
                        AvatarDecision.parse(
                                "{\"intent\":\"explain\",\"emotion\":\"neutral\",\"motionId\":\"explain\"}",
                                mapper));
    }

    @Test
    void denialIsAValidSemanticIntent() throws Exception {
        assertEquals(
                "deny",
                AvatarDecision.parse("{\"intent\":\"deny\",\"emotion\":\"neutral\"}", mapper)
                        .intent());
        assertTrue(
                AvatarDecision.schema(mapper)
                        .path("properties")
                        .path("intent")
                        .path("enum")
                        .toString()
                        .contains("\"deny\""));
    }

    @Test
    void explanationCarriesOpeningCueAndKeepsCooldownAfterAnotherIntentPlays() throws Exception {
        var clock = new AtomicLong(0);
        try (var timed = new AvatarDecisionSession(client, mapper, events::add, clock::get)) {
            long[] times = {0, 10, 20, 31};
            for (int i = 0; i < times.length; i++) {
                clock.set(times[i] * 1_000_000_000L);
                reply = new CompletableFuture<>();
                String id = "explanation-" + i;
                var created = mapper.createObjectNode().put("type", "response.created");
                created.putObject("response").put("id", id);
                timed.accept(created);
                timed.accept(
                        mapper.createObjectNode()
                                .put("type", "response.audio_transcript.delta")
                                .put("response_id", id)
                                .put("delta", "因为不同波长的散射程度不一样。"));
                String intent = i == 1 ? "greet" : "explain";
                assertEquals(i + 1, inputs.size(), "only one classification per response");
                assertTrue(reply.complete(new AvatarDecision(intent, "neutral")));
                var offered = events.get(i);
                assertEquals(i == 2 ? "none" : intent, offered.path("intent").asText());
                assertEquals("opening", offered.path("segmentId").asText());
                assertEquals(1500, offered.path("expiresInMs").asInt());
                if (i != 2)
                    timed.accept(
                            mapper.createObjectNode()
                                    .put("type", "avatar.gesture.started")
                                    .put("responseId", id)
                                    .put("cueId", "opening")
                                    .put("intent", intent)
                                    .put("motionId", i == 1 ? "wave" : "explain"));
            }
        }
    }

    @Test
    void cooldownTracksEachIntentEvenAfterAnotherIntentPlays() throws Exception {
        var clock = new AtomicLong(0);
        try (var timed = new AvatarDecisionSession(client, mapper, events::add, clock::get)) {
            for (int i = 0; i < 3; i++) {
                clock.set(i * 10_000_000_000L);
                String id = "r" + i;
                String intent = i == 1 ? "agree" : "greet";
                reply = new CompletableFuture<>();
                timed.accept(
                        mapper.readTree(
                                "{\"type\":\"response.created\",\"response\":{\"id\":\""
                                        + id
                                        + "\"}}"));
                timed.accept(
                        mapper.readTree(
                                "{\"type\":\"response.audio_transcript.delta\",\"response_id\":\""
                                        + id
                                        + "\",\"delta\":\"Hello!\"}"));
                reply.complete(new AvatarDecision(intent, "happy"));
                assertEquals(i == 2 ? "none" : intent, events.get(i).path("intent").asText());
                timed.accept(
                        mapper.readTree(
                                "{\"type\":\"avatar.gesture.started\",\"responseId\":\""
                                        + id
                                        + "\",\"cueId\":\"opening\",\"intent\":\""
                                        + intent
                                        + "\",\"motionId\":\"test_clip\"}"));
            }
        }
    }

    @Test
    void expiredAcknowledgementCannotConsumeCooldown() throws Exception {
        var clock = new AtomicLong(0);
        try (var timed = new AvatarDecisionSession(client, mapper, events::add, clock::get)) {
            timed.accept(
                    mapper.readTree(
                            "{\"type\":\"response.created\",\"response\":{\"id\":\"r1\"}}"));
            timed.accept(
                    mapper.readTree(
                            "{\"type\":\"response.audio_transcript.delta\",\"response_id\":\"r1\",\"delta\":\"Hello!\"}"));
            reply.complete(new AvatarDecision("greet", "happy"));
            clock.set(2_000_000_000L);
            timed.accept(
                    mapper.readTree(
                            "{\"type\":\"avatar.gesture.started\",\"responseId\":\"r1\",\"cueId\":\"opening\",\"intent\":\"greet\",\"motionId\":\"wave\"}"));
            reply = new CompletableFuture<>();
            timed.accept(
                    mapper.readTree(
                            "{\"type\":\"response.created\",\"response\":{\"id\":\"r2\"}}"));
            timed.accept(
                    mapper.readTree(
                            "{\"type\":\"response.audio_transcript.delta\",\"response_id\":\"r2\",\"delta\":\"Hello!\"}"));
            assertEquals("none", inputs.get(1).recentIntent());
        }
    }
}
