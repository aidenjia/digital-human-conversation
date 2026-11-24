package con.aidenjia.digitalhuman.conversation.api.convoai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.ConvoaiJoinCredentials;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.DigitalHumanType;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.LeaveConvoaiResult;
import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import con.aidenjia.digitalhuman.conversation.config.ResourceServerSecurityConfig;
import con.aidenjia.digitalhuman.conversation.config.WebSecurityConfig;
import con.aidenjia.digitalhuman.conversation.exception.GlobalExceptionHandler;
import con.aidenjia.digitalhuman.conversation.trtc.ConvoaiService;
import con.aidenjia.digitalhuman.conversation.trtc.command.JoinConvoaiCommand;
import con.aidenjia.digitalhuman.conversation.trtc.command.LeaveConvoaiCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConvoaiController.class)
@Import({
    WebSecurityConfig.class,
    ResourceServerSecurityConfig.class,
    GlobalExceptionHandler.class,
    ApiResponseWriter.class,
    con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiJoinGuard.class,
    con.aidenjia.digitalhuman.conversation.security.CurrentUserIdAccessor.class
})
@EnableConfigurationProperties(ApiSecurityProperties.class)
@TestPropertySource(
        properties = {
            "api.security.issuer-uri=https://test-issuer/",
            "api.security.jwk-set-uri=https://test-issuer/.well-known/jwks"
        })
class ConvoaiControllerTest {

    private static final String AUTH_HEADER = "Authorization";

    private static final String VALID_BEARER = "Bearer valid-token";

    private static final String JOIN_BODY =
            """

            {"deviceId":"device-abc-123","digitalHumanType":"Unity"}

            """;

    private static final String LEAVE_BODY =
            """

            {"agentId":"agent-1"}

            """;

    @Autowired private MockMvc mockMvc;

    @MockBean private ConvoaiService convoaiService;

    @MockBean private JwtDecoder jwtDecoder;

    @BeforeEach
    void stubValidJwt() {
        when(jwtDecoder.decode(anyString()))
                .thenAnswer(
                        invocation -> {
                            String token = invocation.getArgument(0, String.class);
                            if ("valid-token".equals(token)) {
                                return Jwt.withTokenValue(token)
                                        .header("alg", "RS256")
                                        .issuer("https://test-issuer/")
                                        .subject("user-1")
                                        .build();
                            }
                            throw new BadJwtException("invalid");
                        });
    }

    @Test
    @DisplayName("鉴权通过且请求体合法时返回 RTC 进房凭据")
    void joinReturnsCredentials() throws Exception {

        when(convoaiService.join(any(JoinConvoaiCommand.class)))
                .thenReturn(
                        new ConvoaiJoinCredentials(
                                "app-1", "room-001", "rtc-token", "abc123userid", "agent-1"));

        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header(AUTH_HEADER, VALID_BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appId").value("app-1"))
                .andExpect(jsonPath("$.data.channel").value("room-001"))
                .andExpect(jsonPath("$.data.token").value("rtc-token"))
                .andExpect(jsonPath("$.data.userId").value("abc123userid"))
                .andExpect(jsonPath("$.data.agentId").value("agent-1"));

        verify(convoaiService)
                .join(new JoinConvoaiCommand("device-abc-123", DigitalHumanType.Unity, null));
    }

    @Test
    @DisplayName("join 透传可选 timezone")
    void joinForwardsOptionalTimezone() throws Exception {
        when(convoaiService.join(any(JoinConvoaiCommand.class)))
                .thenReturn(
                        new ConvoaiJoinCredentials(
                                "app-1", "room-001", "rtc-token", "abc123userid", "agent-1"));

        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header(AUTH_HEADER, VALID_BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"deviceId":"device-abc-123","digitalHumanType":"Unity","timezone":"Asia/Hong_Kong"}
                                """))
                .andExpect(status().isOk());

        verify(convoaiService)
                .join(
                        new JoinConvoaiCommand(
                                "device-abc-123", DigitalHumanType.Unity, "Asia/Hong_Kong"));
    }

    @Test
    void overlappingJoinReturnsConflictEnvelopeWithoutCallingServiceAgain() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(convoaiService.join(any(JoinConvoaiCommand.class)))
                .thenAnswer(
                        invocation -> {
                            entered.countDown();
                            org.assertj.core.api.Assertions.assertThat(
                                            release.await(5, java.util.concurrent.TimeUnit.SECONDS))
                                    .isTrue();
                            return new ConvoaiJoinCredentials(
                                    "app", "room", "token", "user-1", "agent");
                        });
        try (var pool = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var first =
                    pool.submit(
                            () ->
                                    mockMvc.perform(
                                                    post("/api/convoai/agents")
                                                            .header(AUTH_HEADER, VALID_BEARER)
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .content(JOIN_BODY))
                                            .andExpect(status().isOk())
                                            .andReturn());
            try {
                org.assertj.core.api.Assertions.assertThat(
                                entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
                        .isTrue();
                mockMvc.perform(
                                post("/api/convoai/agents")
                                        .header(AUTH_HEADER, VALID_BEARER)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(JOIN_BODY))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").value(409))
                        .andExpect(jsonPath("$.success").value(false));
            } finally {
                release.countDown();
            }
            first.get(5, java.util.concurrent.TimeUnit.SECONDS);
            verify(convoaiService, org.mockito.Mockito.times(1))
                    .join(any(JoinConvoaiCommand.class));
        }
    }

    @Test
    @DisplayName("join 忽略请求体多余的 businessCode")
    void joinIgnoresUnknownBusinessCodeField() throws Exception {
        when(convoaiService.join(any(JoinConvoaiCommand.class)))
                .thenReturn(
                        new ConvoaiJoinCredentials(
                                "app-1", "room-001", "rtc-token", "abc123userid", "agent-1"));

        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header(AUTH_HEADER, VALID_BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"deviceId":"device-abc-123","digitalHumanType":"UE5","businessCode":"UE5_001"}
                                """))
                .andExpect(status().isOk());

        verify(convoaiService)
                .join(new JoinConvoaiCommand("device-abc-123", DigitalHumanType.UE5, null));
    }

    @Test
    @DisplayName("非法 digitalHumanType 时返回 400 与字段错误")
    void joinRejectsInvalidDigitalHumanType() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header(AUTH_HEADER, VALID_BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"deviceId":"device-abc-123","digitalHumanType":"avatar-2d"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(
                        jsonPath("$.error.errors.digitalHumanType")
                                .value("digitalHumanType must be UE5, Unity, or MMD"));
    }

    @Test
    @DisplayName("鉴权通过时按 agentId 停止智能体")
    void leaveReturnsResult() throws Exception {

        when(convoaiService.leave(any(LeaveConvoaiCommand.class)))
                .thenReturn(new LeaveConvoaiResult("agent-1", "STOPPED"));

        mockMvc.perform(
                        delete("/api/convoai/agents")
                                .header(AUTH_HEADER, VALID_BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(LEAVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agentId").value("agent-1"))
                .andExpect(jsonPath("$.data.status").value("STOPPED"));

        verify(convoaiService).leave(new LeaveConvoaiCommand("agent-1"));
    }

    @Test
    @DisplayName("无 Authorization 返回 401/401")
    void missingAuthorizationReturns401() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Cannot authenticate"));
    }

    @Test
    @DisplayName("无效 JWT 返回 401/4010")
    void invalidJwtReturns4010() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header(AUTH_HEADER, "Bearer wrong-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4010))
                .andExpect(jsonPath("$.message").value("Token is invalid"));
    }
}
