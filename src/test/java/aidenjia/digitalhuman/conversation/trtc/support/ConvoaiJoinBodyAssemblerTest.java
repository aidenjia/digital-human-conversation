package con.aidenjia.digitalhuman.conversation.trtc.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import con.aidenjia.digitalhuman.conversation.api.convoai.dto.DigitalHumanType;
import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import con.aidenjia.digitalhuman.conversation.config.TencentCloudCredentialsProperties;
import con.aidenjia.digitalhuman.conversation.trtc.command.JoinConvoaiCommand;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class ConvoaiJoinBodyAssemblerTest {

    private ConvoaiProperties props;
    private ConvoaiJoinBodyAssembler assembler;

    @BeforeEach
    void setUp() {
        props = new ConvoaiProperties();
        props.setAppId("app-1");
        props.setAgentRtcUid("agent");
        props.setIdleTimeout(60);
        props.setEnableStringUid(true);
        props.getLlm().setUrl("https://example.com/llm");
        props.getLlm().setApiKey("llm-key");
        props.getTts().setAppId("tts-app");
        props.getTts().setVoiceType(1001);

        TencentCloudCredentialsProperties credentials = new TencentCloudCredentialsProperties();
        credentials.setSecretId("sid");
        credentials.setSecretKey("skey");
        assembler = new ConvoaiJoinBodyAssembler(credentials);
    }

    @Test
    @DisplayName("yml 画像：llm.params.replyLanguage 为 zh-cn")
    void ymlProfileSetsDefaultReplyLanguage() {
        JoinConvoaiCommand command =
                new JoinConvoaiCommand("device-1", DigitalHumanType.Unity, null);
        Map<String, Object> body =
                assembler.build(
                        props,
                        command,
                        "channel-1",
                        "user-1",
                        "agent-token",
                        JoinSessionProfile.CONFIGURED);

        Map<String, Object> llmParams = llmParams(body);
        assertEquals("zh-cn", llmParams.get("replyLanguage"));
        assertTrue((Boolean) llmParams.get("stream"));
        assertEquals("zh-CN", asrLanguage(body));
    }

    private static String asrLanguage(Map<String, Object> body) {
        Map<String, Object> properties = (Map<String, Object>) body.get("properties");
        Map<String, Object> asr = (Map<String, Object>) properties.get("asr");
        return (String) asr.get("language");
    }

    @Test
    void configuredComfortMessageIsIncludedInSilenceConfig() {
        props.setComfortMessage("Still there?");
        props.setSilenceTimeoutMs(15000);
        props.setSilenceAction("speak");
        Map<String, Object> parameters = assembledParameters();
        assertEquals(
                Map.of("timeout_ms", 15000, "action", "speak", "content", "Still there?"),
                parameters.get("silence_config"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {" ", "\t"})
    void blankComfortMessageOmitsSilenceConfig(String comfort) {
        props.setComfortMessage(comfort);
        props.setSilenceTimeoutMs(15000);
        assertTrue(!assembledParameters().containsKey("silence_config"));
    }

    @Test
    void disabledSilenceTimeoutOmitsConfiguredComfort() {
        props.setComfortMessage("Still there?");
        props.setSilenceTimeoutMs(0);
        assertTrue(!assembledParameters().containsKey("silence_config"));
    }

    private Map<String, Object> assembledParameters() {
        Map<String, Object> body =
                assembler.build(
                        props,
                        new JoinConvoaiCommand("device-1", DigitalHumanType.Unity, null),
                        "channel-1",
                        "user-1",
                        "agent-token",
                        JoinSessionProfile.CONFIGURED);
        return (Map<String, Object>)
                ((Map<String, Object>) body.get("properties")).get("parameters");
    }

    private static Map<String, Object> llmParams(Map<String, Object> body) {
        Map<String, Object> properties = (Map<String, Object>) body.get("properties");
        Map<String, Object> llm = (Map<String, Object>) properties.get("llm");
        return (Map<String, Object>) llm.get("params");
    }
}
