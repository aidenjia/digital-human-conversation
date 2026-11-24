package con.aidenjia.digitalhuman.conversation.api.convoai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.ConvoaiJoinCredentials;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.LeaveConvoaiResult;
import con.aidenjia.digitalhuman.conversation.api.rtc.RtcCredentialsController;
import con.aidenjia.digitalhuman.conversation.api.rtc.dto.RtcCredentialsResponse;
import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import con.aidenjia.digitalhuman.conversation.config.ResourceServerSecurityConfig;
import con.aidenjia.digitalhuman.conversation.config.WebSecurityConfig;
import con.aidenjia.digitalhuman.conversation.exception.GlobalExceptionHandler;
import con.aidenjia.digitalhuman.conversation.rtc.RtcCredentialsService;
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

@WebMvcTest({ConvoaiController.class, RtcCredentialsController.class})
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
            "api.security.jwk-set-uri=https://test-issuer/.well-known/jwks",
            "api.security.demo-token-enabled=true",
            "api.security.demo-token=atk_demo_fixed",
            "api.security.demo-user-id=demo-ue5"
        })
class DemoBearerAuthTest {

    private static final String JOIN_BODY =
            """
            {"deviceId":"device-abc-123","digitalHumanType":"Unity"}
            """;

    private static final String LEAVE_BODY = """
            {"agentId":"agent-1"}
            """;

    @Autowired private MockMvc mockMvc;

    @MockBean private ConvoaiService convoaiService;

    @MockBean private RtcCredentialsService rtcCredentialsService;

    @MockBean private JwtDecoder jwtDecoder;

    @BeforeEach
    void stubJwtDecoderRejectsNonDemo() {
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
    @DisplayName("demo：固定 token 可 join")
    void demoTokenAllowsJoin() throws Exception {
        when(convoaiService.join(any(JoinConvoaiCommand.class)))
                .thenReturn(
                        new ConvoaiJoinCredentials(
                                "app-1", "room-001", "rtc-token", "demo-ue5", "agent-1"));

        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header("Authorization", "Bearer atk_demo_fixed")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("demo：固定 token 可 leave")
    void demoTokenAllowsLeave() throws Exception {
        when(convoaiService.leave(any(LeaveConvoaiCommand.class)))
                .thenReturn(new LeaveConvoaiResult("agent-1", "STOPPED"));

        mockMvc.perform(
                        delete("/api/convoai/agents")
                                .header("Authorization", "Bearer atk_demo_fixed")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(LEAVE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("demo：错误固定串仍 4010")
    void wrongDemoTokenRejected() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header("Authorization", "Bearer wrong-demo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4010));
    }

    @Test
    @DisplayName("demo：固定 token 可 POST /api/rtc/credentials")
    void demoTokenAllowsRtcCredentials() throws Exception {
        when(rtcCredentialsService.issue())
                .thenReturn(
                        new RtcCredentialsResponse(
                                "app-1", "channel-demo", "rtc-token", "demo-ue5"));

        mockMvc.perform(
                        post("/api/rtc/credentials")
                                .header("Authorization", "Bearer atk_demo_fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("demo-ue5"));
    }

    @Test
    @DisplayName("demo：其它 /api 路径不接受固定 token")
    void demoTokenRejectedOnOtherApi() throws Exception {
        mockMvc.perform(
                        get("/api/__auth_default_probe")
                                .header("Authorization", "Bearer atk_demo_fixed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4010));
    }

    @Test
    @DisplayName("demo：开启时真 JWT 仍可 join")
    void realJwtStillWorksWhenDemoEnabled() throws Exception {
        when(convoaiService.join(any(JoinConvoaiCommand.class)))
                .thenReturn(
                        new ConvoaiJoinCredentials(
                                "app-1", "room-001", "rtc-token", "user-1", "agent-1"));

        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
