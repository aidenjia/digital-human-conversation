package con.aidenjia.digitalhuman.conversation.trtc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import con.aidenjia.digitalhuman.conversation.api.convoai.dto.DigitalHumanType;
import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import con.aidenjia.digitalhuman.conversation.config.TencentCloudCredentialsProperties;
import con.aidenjia.digitalhuman.conversation.exception.ApiException;
import con.aidenjia.digitalhuman.conversation.exception.ConfigurationException;
import con.aidenjia.digitalhuman.conversation.exception.UpstreamException;
import con.aidenjia.digitalhuman.conversation.infrastructure.agent.AgentSessionWarmClient;
import con.aidenjia.digitalhuman.conversation.infrastructure.convoai.AgoraRestClient;
import con.aidenjia.digitalhuman.conversation.infrastructure.convoai.ConvoaiClientFactory;
import con.aidenjia.digitalhuman.conversation.security.CurrentUserIdAccessor;
import con.aidenjia.digitalhuman.conversation.trtc.command.JoinConvoaiCommand;
import con.aidenjia.digitalhuman.conversation.trtc.command.LeaveConvoaiCommand;
import con.aidenjia.digitalhuman.conversation.trtc.support.AgoraRtcTokenService;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiAgentOwnershipStore;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiJoinBodyAssembler;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiLogContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConvoaiServiceTest {

    private static final JoinConvoaiCommand JOIN_COMMAND =
            new JoinConvoaiCommand("device-abc-123", DigitalHumanType.Unity, null);

    @Mock private AgoraRestClient restClient;

    @Mock private ConvoaiClientFactory clientFactory;

    @Mock private AgoraRtcTokenService rtcTokenService;

    @Mock private AgentSessionWarmClient sessionWarmClient;

    @Mock private CurrentUserIdAccessor currentUserIdAccessor;

    @Mock private ConvoaiAgentOwnershipStore agentOwnershipStore;

    private ConvoaiProperties props;
    private TencentCloudCredentialsProperties credentials;
    private ConvoaiService service;

    @AfterEach
    void tearDown() {
        ConvoaiLogContext.clear();
    }

    @BeforeEach
    void setUp() {
        props = new ConvoaiProperties();
        props.setAppId("app-1");
        props.setUsername("user");
        props.setPassword("pass");
        props.setAgentRtcUid("agent");
        props.setIdleTimeout(60);
        props.setDomainArea("CN");
        props.setServerRegion("CHINESE_MAINLAND");
        props.setEnableStringUid(true);

        ConvoaiProperties.Llm llm = new ConvoaiProperties.Llm();
        llm.setUrl("https://example.com/llm");
        llm.setApiKey("llm-key");
        props.setLlm(llm);

        ConvoaiProperties.Tts tts = new ConvoaiProperties.Tts();
        tts.setAppId("tts-app");
        tts.setVoiceType(1001);
        tts.setVolume(5);
        tts.setSpeed(1);
        props.setTts(tts);

        credentials = new TencentCloudCredentialsProperties();
        credentials.setSecretId("tts-sid");
        credentials.setSecretKey("tts-skey");

        service =
                new ConvoaiService(
                        restClient,
                        clientFactory,
                        rtcTokenService,
                        new ConvoaiJoinBodyAssembler(credentials),
                        currentUserIdAccessor,
                        sessionWarmClient,
                        agentOwnershipStore);
        lenient().when(currentUserIdAccessor.requireUserId()).thenReturn("user-1");
        lenient()
                .when(agentOwnershipStore.findOwner(anyString()))
                .thenReturn(Optional.of("user-1"));
    }

    @Test
    @DisplayName("LLM URL 缺失时抛 ConfigurationException")
    void joinThrowsWhenLlmUrlMissing() {
        props.getLlm().setUrl(null);
        when(clientFactory.getProperties()).thenReturn(props);

        ConfigurationException ex =
                assertThrows(ConfigurationException.class, () -> service.join(JOIN_COMMAND));
        assertTrue(ex.getMessage().contains("ConvoAI LLM url"));
    }

    @Test
    @DisplayName("配置缺失时抛出 ConfigurationException")
    void joinThrowsWhenConfigurationInvalid() {
        doThrow(new ConfigurationException("ConvoAI app id is not configured"))
                .when(clientFactory)
                .validateForJoin();

        assertThrows(ConfigurationException.class, () -> service.join(JOIN_COMMAND));
    }

    @Test
    @DisplayName("TTS 凭证缺失时抛 ConfigurationException")
    void joinThrowsWhenTtsCredentialsMissing() {
        credentials.setSecretId("");
        when(clientFactory.getProperties()).thenReturn(props);

        ConfigurationException ex =
                assertThrows(ConfigurationException.class, () -> service.join(JOIN_COMMAND));
        assertTrue(ex.getMessage().contains("ConvoAI Tencent TTS"));
    }

    @Test
    @DisplayName("REST API 返回成功时返回 RTC 进房凭据与 agentId")
    void joinReturnsCredentials() {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);
        when(sessionWarmClient.warm(
                        anyString(), anyString(), anyString(), anyString(), isNull(), anyString()))
                .thenReturn(true);

        var credentials = service.join(JOIN_COMMAND);

        assertEquals("app-1", credentials.appId());
        assertEquals(32, credentials.channel().length());
        assertEquals("user-token", credentials.token());
        assertEquals("user-1", credentials.userId());
        assertEquals("agent-xyz", credentials.agentId());
        verify(rtcTokenService).buildUserToken(eq(credentials.channel()), eq("user-1"));
        verify(sessionWarmClient)
                .warm(
                        eq(credentials.userId()),
                        eq(credentials.channel()),
                        eq(JOIN_COMMAND.deviceId()),
                        eq("Unity"),
                        isNull(),
                        eq("zh-cn"));
    }

    @Test
    @DisplayName("warm 失败时 join 仍返回凭据")
    void joinReturnsCredentialsWhenWarmFails() {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);
        when(sessionWarmClient.warm(
                        anyString(), anyString(), anyString(), anyString(), isNull(), anyString()))
                .thenReturn(false);

        var credentials = service.join(JOIN_COMMAND);

        assertEquals("agent-xyz", credentials.agentId());
        verify(sessionWarmClient)
                .warm(anyString(), anyString(), anyString(), anyString(), isNull(), eq("zh-cn"));
    }

    @Test
    @DisplayName("warm 抛异常时 join 仍返回凭据")
    void joinReturnsCredentialsWhenWarmThrows() {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);
        when(sessionWarmClient.warm(
                        anyString(), anyString(), anyString(), anyString(), isNull(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        var credentials = service.join(JOIN_COMMAND);

        assertEquals("agent-xyz", credentials.agentId());
    }

    @Test
    @DisplayName("session-warm 关闭时不调用 warm")
    void joinSkipsWarmWhenDisabled() {
        props.getSessionWarm().setEnabled(false);
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);

        var credentials = service.join(JOIN_COMMAND);

        assertEquals("agent-xyz", credentials.agentId());
        verify(sessionWarmClient, never())
                .warm(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Agora join 失败时不调用 warm")
    void joinDoesNotWarmWhenAgoraFails() {
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenThrow(new UpstreamException("Agora ConvoAI join failed"));

        assertThrows(UpstreamException.class, () -> service.join(JOIN_COMMAND));
        verify(sessionWarmClient, never())
                .warm(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("未开启 Token 鉴权时 token 为 null")
    void joinReturnsNullTokenWhenAuthDisabled() {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn(null);
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn(null);
        when(restClient.join(any())).thenReturn(stub);
        when(sessionWarmClient.warm(
                        anyString(), anyString(), anyString(), anyString(), isNull(), anyString()))
                .thenReturn(true);

        var credentials = service.join(JOIN_COMMAND);

        assertNull(credentials.token());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("请求体包含 turn_detection 与 interruption 配置")
    void joinSendsTurnDetectionAndInterruption() {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);

        service.join(JOIN_COMMAND);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).join(captor.capture());
        Map<String, Object> body = captor.getValue();
        Map<String, Object> properties = (Map<String, Object>) body.get("properties");

        // turn_detection
        Map<String, Object> turnDetection = (Map<String, Object>) properties.get("turn_detection");
        assertEquals("default", turnDetection.get("mode"));

        Map<String, Object> config = (Map<String, Object>) turnDetection.get("config");
        assertEquals(0.5F, config.get("speech_threshold"));

        Map<String, Object> startOfSpeech = (Map<String, Object>) config.get("start_of_speech");
        assertEquals("vad", startOfSpeech.get("mode"));

        Map<String, Object> endOfSpeech = (Map<String, Object>) config.get("end_of_speech");
        assertEquals("vad", endOfSpeech.get("mode"));

        Map<String, Object> sosVad = (Map<String, Object>) startOfSpeech.get("vad_config");
        assertEquals(160, sosVad.get("interrupt_duration_ms"));
        assertEquals(280, sosVad.get("speaking_interrupt_duration_ms"));

        Map<String, Object> eosVad = (Map<String, Object>) endOfSpeech.get("vad_config");
        assertEquals(320, eosVad.get("silence_duration_ms"));

        // vad_config 直接在 start_of_speech 下，没有多余 config 嵌套
        assertNotNull(startOfSpeech.get("vad_config"));
        assertNotNull(endOfSpeech.get("vad_config"));

        Map<String, Object> parameters = (Map<String, Object>) properties.get("parameters");
        assertEquals("aiserver", parameters.get("audio_scenario"));

        // interruption（开口打断；不传 keywords_config）
        Map<String, Object> interruption = (Map<String, Object>) properties.get("interruption");
        assertEquals(true, interruption.get("enable"));
        assertEquals("start_of_speech", interruption.get("mode"));
        assertFalse(interruption.containsKey("keywords_config"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("请求体 LLM 包含 deviceId 与 digitalHumanType")
    void joinSendsLlmParams() {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);

        service.join(JOIN_COMMAND);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).join(captor.capture());
        Map<String, Object> body = captor.getValue();
        Map<String, Object> properties = (Map<String, Object>) body.get("properties");
        Map<String, Object> llm = (Map<String, Object>) properties.get("llm");

        assertEquals("https://example.com/llm", llm.get("url"));
        assertEquals("llm-key", llm.get("api_key"));
        assertEquals("custom", llm.get("vendor"));
        assertEquals(1, llm.get("max_history"));

        Map<String, Object> params = (Map<String, Object>) llm.get("params");
        assertEquals("device-abc-123", params.get("deviceId"));
        assertEquals("Unity", params.get("digitalHumanType"));
        assertEquals("custom", params.get("model"));
        assertEquals("user-1", params.get("clientUserId"));
        assertEquals("zh-cn", params.get("replyLanguage"));
        assertTrue(!params.containsKey("platformUserId"));
        assertEquals(List.of(params.get("clientUserId")), properties.get("remote_rtc_uids"));
        assertTrue(!params.containsKey("timezone"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("请求体 LLM params 透传 timezone")
    void joinSendsTimezoneInLlmParams() {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);

        service.join(
                new JoinConvoaiCommand(
                        "device-abc-123", DigitalHumanType.Unity, "America/New_York"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).join(captor.capture());
        Map<String, Object> body = captor.getValue();
        Map<String, Object> properties = (Map<String, Object>) body.get("properties");
        Map<String, Object> llm = (Map<String, Object>) properties.get("llm");
        Map<String, Object> params = (Map<String, Object>) llm.get("params");
        assertEquals("America/New_York", params.get("timezone"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("timezone 空白时不写入 llm params")
    void joinOmitsBlankTimezone() {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);

        service.join(new JoinConvoaiCommand("device-abc-123", DigitalHumanType.Unity, "   "));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).join(captor.capture());
        Map<String, Object> body = captor.getValue();
        Map<String, Object> properties = (Map<String, Object>) body.get("properties");
        Map<String, Object> llm = (Map<String, Object>) properties.get("llm");
        Map<String, Object> params = (Map<String, Object>) llm.get("params");
        assertTrue(!params.containsKey("timezone"));
    }

    @SuppressWarnings("unchecked")
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(DigitalHumanType.class)
    void joinUsesConfiguredProfileForEveryDigitalHumanType(DigitalHumanType type) {
        var stub = new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING");
        props.setIdleTimeout(180);
        props.setGreetingMessage("yml-hello");
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenReturn(stub);

        service.join(new JoinConvoaiCommand("device-1", type, "Asia/Shanghai"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(restClient).join(captor.capture());
        Map<String, Object> properties = (Map<String, Object>) captor.getValue().get("properties");
        assertEquals(180, properties.get("idle_timeout"));
        Map<String, Object> llm = (Map<String, Object>) properties.get("llm");
        assertEquals("yml-hello", llm.get("greeting_message"));
        Map<String, Object> params = (Map<String, Object>) llm.get("params");
        assertEquals(type.name(), params.get("digitalHumanType"));
        assertEquals("zh-cn", params.get("replyLanguage"));
        verify(sessionWarmClient)
                .warm(
                        anyString(),
                        anyString(),
                        eq("device-1"),
                        eq(type.name()),
                        eq("Asia/Shanghai"),
                        eq("zh-cn"));
        Map<String, Object> tts = (Map<String, Object>) properties.get("tts");
        Map<String, Object> ttsParams = (Map<String, Object>) tts.get("params");
        assertEquals(1001, ttsParams.get("voice_type"));
        assertEquals(5, ttsParams.get("volume"));
        assertEquals(24000, ttsParams.get("sample_rate"));
    }

    @Test
    @DisplayName("REST API 抛 UpstreamException 时透传")
    void joinTranslatesUpstreamException() {
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any())).thenThrow(new UpstreamException("Agora ConvoAI join failed"));

        assertThrows(UpstreamException.class, () -> service.join(JOIN_COMMAND));
    }

    @Test
    @DisplayName("leave 成功时返回 LeaveConvoaiResult")
    void leaveReturnsResult() {
        var result = service.leave(new LeaveConvoaiCommand("agent-1"));

        assertEquals("agent-1", result.agentId());
        assertEquals("STOPPED", result.status());
        verify(restClient).leave("agent-1");
        verify(agentOwnershipStore).unbind("agent-1");
    }

    @Test
    @DisplayName("leave 上游失败时透传 UpstreamException")
    void leavePropagatesUpstreamException() {
        doThrow(new UpstreamException("Agora ConvoAI leave returned HTTP 404"))
                .when(restClient)
                .leave("agent-1");

        assertThrows(
                UpstreamException.class, () -> service.leave(new LeaveConvoaiCommand("agent-1")));
        verify(agentOwnershipStore, never()).unbind(anyString());
    }

    @Test
    @DisplayName("leave 他人 agent 时 404 且不调 Agora")
    void leaveDeniesWhenOwnedByOtherUser() {
        when(agentOwnershipStore.findOwner("agent-1")).thenReturn(Optional.of("user-other"));

        ApiException ex =
                assertThrows(
                        ApiException.class,
                        () -> service.leave(new LeaveConvoaiCommand("agent-1")));

        assertEquals(404, ex.getCode());
        verify(restClient, never()).leave(anyString());
        verify(agentOwnershipStore, never()).unbind(anyString());
    }

    @Test
    @DisplayName("leave 无归属映射时 404 且不调 Agora")
    void leaveDeniesWhenOwnershipMissing() {
        when(agentOwnershipStore.findOwner("agent-1")).thenReturn(Optional.empty());

        ApiException ex =
                assertThrows(
                        ApiException.class,
                        () -> service.leave(new LeaveConvoaiCommand("agent-1")));

        assertEquals(404, ex.getCode());
        verify(restClient, never()).leave(anyString());
    }

    @Test
    @DisplayName("join 成功后登记 agent 归属")
    void joinBindsAgentOwnership() {
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any()))
                .thenReturn(new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING"));
        props.getSessionWarm().setEnabled(false);

        service.join(JOIN_COMMAND);

        verify(agentOwnershipStore).bind("agent-xyz", "user-1");
    }

    @Test
    void realMemoryStoreAllowsOwnerToLeaveButRejectsOtherUsers() {
        var owners =
                new con.aidenjia.digitalhuman.conversation.trtc.support
                        .InMemoryConvoaiAgentOwnershipStore(86400, java.time.Clock.systemUTC());
        var localService =
                new ConvoaiService(
                        restClient,
                        clientFactory,
                        rtcTokenService,
                        new ConvoaiJoinBodyAssembler(credentials),
                        currentUserIdAccessor,
                        sessionWarmClient,
                        owners);
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any()))
                .thenReturn(new AgoraRestClient.JoinResponse("local-agent", 0, "RUNNING"));
        props.getSessionWarm().setEnabled(false);
        localService.join(JOIN_COMMAND);
        when(currentUserIdAccessor.requireUserId()).thenReturn("other-user");
        assertEquals(
                404,
                assertThrows(
                                ApiException.class,
                                () -> localService.leave(new LeaveConvoaiCommand("local-agent")))
                        .getHttpStatus());
        verify(restClient, never()).leave(anyString());
        when(currentUserIdAccessor.requireUserId()).thenReturn("user-1");
        assertEquals(
                "STOPPED", localService.leave(new LeaveConvoaiCommand("local-agent")).status());
        assertTrue(owners.findOwner("local-agent").isEmpty());
        verify(restClient).leave("local-agent");
    }

    @Test
    @DisplayName("join 归属登记失败时仍返回凭据")
    void joinReturnsCredentialsWhenOwnershipBindFails() {
        when(clientFactory.getProperties()).thenReturn(props);
        when(rtcTokenService.buildAgentToken(any())).thenReturn("agent-token");
        when(rtcTokenService.buildUserToken(any(), any())).thenReturn("user-token");
        when(restClient.join(any()))
                .thenReturn(new AgoraRestClient.JoinResponse("agent-xyz", 1737111452, "RUNNING"));
        props.getSessionWarm().setEnabled(false);
        doThrow(new RuntimeException("state store failed"))
                .when(agentOwnershipStore)
                .bind(any(), any());

        var credentials = service.join(JOIN_COMMAND);

        assertEquals("agent-xyz", credentials.agentId());
    }
}
