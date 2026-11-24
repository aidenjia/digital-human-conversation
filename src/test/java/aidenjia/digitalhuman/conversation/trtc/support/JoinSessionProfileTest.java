package con.aidenjia.digitalhuman.conversation.trtc.support;

import static org.assertj.core.api.Assertions.assertThat;

import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JoinSessionProfileTest {
    @Test
    void configurationControlsGreetingAndVoiceWithoutComfortOrPitch() {
        ConvoaiProperties props = new ConvoaiProperties();
        props.setGreetingMessage("hello");
        props.getTts().setVoiceType(1001);
        props.getTts().setVolume(1);
        props.getTts().setSpeed(2);
        JoinSessionProfile profile = JoinSessionProfile.CONFIGURED;
        assertThat(profile.greetingMessage(props)).contains("hello");
        assertThat(profile.comfortText(props)).isEmpty();
        assertThat(profile.replyLanguage()).isEqualTo("zh-cn");
        Map<String, Object> params = new LinkedHashMap<>();
        profile.putTtsVoiceAndScale(params, props);
        assertThat(params)
                .containsEntry("voice_type", 1001)
                .containsEntry("volume", 1)
                .containsEntry("speed", 2)
                .doesNotContainKey("pitch");
    }

    @Test
    void blankGreetingIsOmitted() {
        ConvoaiProperties props = new ConvoaiProperties();
        props.setGreetingMessage(" ");
        assertThat(JoinSessionProfile.CONFIGURED.greetingMessage(props)).isEmpty();
        props.setGreetingMessage(null);
        assertThat(JoinSessionProfile.CONFIGURED.greetingMessage(props)).isEmpty();
    }

    @Test
    void configuredComfortMessageIsUsed() {
        ConvoaiProperties props = new ConvoaiProperties();
        props.setComfortMessage("Still there?");
        assertThat(JoinSessionProfile.CONFIGURED.comfortText(props)).contains("Still there?");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {" ", "\t"})
    void blankComfortMessageIsOmitted(String comfort) {
        ConvoaiProperties props = new ConvoaiProperties();
        props.setComfortMessage(comfort);
        assertThat(JoinSessionProfile.CONFIGURED.comfortText(props)).isEmpty();
    }
}
