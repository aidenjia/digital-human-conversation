package con.aidenjia.digitalhuman.conversation.api.rtc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import con.aidenjia.digitalhuman.conversation.api.rtc.dto.RtcCredentialsResponse;
import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import con.aidenjia.digitalhuman.conversation.config.ResourceServerSecurityConfig;
import con.aidenjia.digitalhuman.conversation.config.WebSecurityConfig;
import con.aidenjia.digitalhuman.conversation.exception.GlobalExceptionHandler;
import con.aidenjia.digitalhuman.conversation.rtc.RtcCredentialsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RtcCredentialsController.class)
@Import({
    WebSecurityConfig.class,
    ResourceServerSecurityConfig.class,
    GlobalExceptionHandler.class,
    ApiResponseWriter.class
})
@EnableConfigurationProperties(ApiSecurityProperties.class)
@TestPropertySource(
        properties = {
            "api.security.issuer-uri=https://test-issuer/",
            "api.security.jwk-set-uri=https://test-issuer/.well-known/jwks",
            "api.security.demo-token-enabled=false"
        })
class RtcCredentialsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private RtcCredentialsService rtcCredentialsService;

    @MockBean private JwtDecoder jwtDecoder;

    @BeforeEach
    void stubJwt() {
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
    @DisplayName("rtc credentials：无 Authorization 返回 401")
    void missingAuthorization() throws Exception {
        mockMvc.perform(post("/api/rtc/credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("rtc credentials：无效 Bearer 返回 4010")
    void invalidBearer() throws Exception {
        mockMvc.perform(post("/api/rtc/credentials").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4010));
    }

    @Test
    @DisplayName("rtc credentials：合法 Bearer 返回凭据且无 agentId")
    void success() throws Exception {
        when(rtcCredentialsService.issue())
                .thenReturn(
                        new RtcCredentialsResponse("app-1", "channel-abc", "rtc-token", "user-1"));

        mockMvc.perform(post("/api/rtc/credentials").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appId").value("app-1"))
                .andExpect(jsonPath("$.data.channel").value("channel-abc"))
                .andExpect(jsonPath("$.data.token").value("rtc-token"))
                .andExpect(jsonPath("$.data.userId").value("user-1"))
                .andExpect(jsonPath("$.data.agentId").doesNotExist());
    }
}
