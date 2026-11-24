package con.aidenjia.digitalhuman.conversation.avatar;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;

/**
 * Semantic intents only; concrete animation IDs are selected by the client. Timing and cue IDs are
 * server-owned.
 */
public record AvatarDecision(String intent, String emotion) {
    private static final Set<String> INTENTS = Set.of("none", "greet", "agree", "deny", "explain");
    private static final Set<String> EMOTIONS =
            Set.of("neutral", "happy", "sad", "angry", "surprised");

    public static final AvatarDecision NONE = new AvatarDecision("none", "neutral");

    public AvatarDecision {
        if (!INTENTS.contains(intent) || !EMOTIONS.contains(emotion)) {
            throw new IllegalArgumentException("Unsupported avatar decision");
        }
    }

    public static JsonNode schema(ObjectMapper mapper) {
        var schema =
                mapper.createObjectNode().put("type", "object").put("additionalProperties", false);
        schema.putArray("required").add("intent").add("emotion");
        var fields = schema.putObject("properties");
        fields.putObject("intent")
                .put("type", "string")
                .putArray("enum")
                .add("none")
                .add("greet")
                .add("agree")
                .add("deny")
                .add("explain");
        var emotions = fields.putObject("emotion").put("type", "string").putArray("enum");
        for (String name : new String[] {"neutral", "happy", "sad", "angry", "surprised"})
            emotions.add(name);
        return schema;
    }

    public static AvatarDecision parse(String json, ObjectMapper mapper) throws IOException {
        try (JsonParser parser = mapper.createParser(json)) {
            parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            JsonNode value = mapper.readTree(parser);
            if (value == null
                    || !value.isObject()
                    || value.size() != 2
                    || !value.path("intent").isTextual()
                    || !value.path("emotion").isTextual()
                    || parser.nextToken() != null)
                throw new IOException("Invalid avatar decision JSON");
            return new AvatarDecision(
                    value.get("intent").textValue(), value.get("emotion").textValue());
        }
    }
}
