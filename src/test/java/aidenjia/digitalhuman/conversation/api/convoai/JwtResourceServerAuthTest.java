package con.aidenjia.digitalhuman.conversation.api.convoai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.ConvoaiJoinCredentials;
import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import con.aidenjia.digitalhuman.conversation.config.ResourceServerSecurityConfig;
import con.aidenjia.digitalhuman.conversation.config.WebSecurityConfig;
import con.aidenjia.digitalhuman.conversation.exception.GlobalExceptionHandler;
import con.aidenjia.digitalhuman.conversation.security.ApiCultureConstants;
import con.aidenjia.digitalhuman.conversation.trtc.ConvoaiService;
import con.aidenjia.digitalhuman.conversation.trtc.command.JoinConvoaiCommand;
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
            "api.security.jwk-set-uri=https://test-issuer/.well-known/jwks",
            "api.security.additional-issuer-uris=https://issuer-prod.example.test/",
            "api.security.demo-token-enabled=false"
        })
class JwtResourceServerAuthTest {

    private static final String JOIN_BODY =
            """

            {"deviceId":"device-abc-123","digitalHumanType":"Unity"}

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
                            if ("additional-issuer-token".equals(token)) {
                                return Jwt.withTokenValue(token)
                                        .header("alg", "RS256")
                                        .issuer("https://issuer-prod.example.test/")
                                        .subject("user-1")
                                        .build();
                            }
                            throw new BadJwtException("invalid");
                        });
    }

    @Test
    @DisplayName("jwt：未登记的 /api 路径无 token 亦拒绝（默认 /api/** 鉴权）")
    void unlistedApiPathRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/__auth_default_probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("jwt：无 Authorization 返回 401/401")
    void missingAuthorization() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Cannot authenticate"));
    }

    @Test
    @DisplayName("jwt：空 Bearer 返回 401/401")
    void emptyBearerToken() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header("Authorization", "Bearer ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("jwt：无效 token 返回 401/4010")
    void invalidToken() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header("Authorization", "Bearer bad-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4010))
                .andExpect(jsonPath("$.message").value("Token is invalid"));
    }

    @Test
    @DisplayName("jwt：无效 token + zh 返回中文 message")
    void invalidTokenChineseMessage() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header("Authorization", "Bearer bad-token")
                                .header(ApiCultureConstants.REQUEST_HEADER, "zh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4010))
                .andExpect(jsonPath("$.message").value("访问令牌已失效"));
    }

    @Test
    @DisplayName("jwt：无 token + zh 返回中文 message")
    void missingAuthorizationChineseMessage() throws Exception {
        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header(ApiCultureConstants.REQUEST_HEADER, "zh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("无法鉴权访问"));
    }

    @Test
    @DisplayName("jwt：合法 token 放行业务")
    void validTokenAllowsRequest() throws Exception {
        when(convoaiService.join(any(JoinConvoaiCommand.class)))
                .thenReturn(
                        new ConvoaiJoinCredentials(
                                "app-1", "room-001", "rtc-token", "abc123userid", "agent-1"));

        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("jwt：AdditionalIssuer 的 iss 可通过校验")
    void additionalIssuerAccepted() throws Exception {
        when(convoaiService.join(any(JoinConvoaiCommand.class)))
                .thenReturn(
                        new ConvoaiJoinCredentials(
                                "app-1", "room-001", "rtc-token", "abc123userid", "agent-1"));

        mockMvc.perform(
                        post("/api/convoai/agents")
                                .header("Authorization", "Bearer additional-issuer-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JOIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
