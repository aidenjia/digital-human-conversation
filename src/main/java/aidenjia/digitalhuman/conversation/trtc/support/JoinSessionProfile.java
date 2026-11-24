package con.aidenjia.digitalhuman.conversation.trtc.support;

import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import java.util.Map;
import java.util.Optional;

/** Configuration-backed voice profile shared by every digital human type. */
public enum JoinSessionProfile {
    CONFIGURED;

    public Optional<String> greetingMessage(ConvoaiProperties props) {
        String greeting = props.getGreetingMessage();
        if (greeting == null || greeting.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(greeting);
    }

    public void putTtsVoiceAndScale(Map<String, Object> params, ConvoaiProperties props) {
        params.put("voice_type", props.getTtsVoiceType());
        params.put("volume", props.getTtsVolume());
        params.put("speed", props.getTtsSpeed());
    }

    public Optional<String> comfortText(ConvoaiProperties props) {
        String comfort = props.getComfortMessage();
        return comfort == null || comfort.isBlank() ? Optional.empty() : Optional.of(comfort);
    }

    public String replyLanguage() {
        return "zh-cn";
    }
}
