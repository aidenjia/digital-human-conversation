package con.aidenjia.digitalhuman.conversation.api.bailian.webrtc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import con.aidenjia.digitalhuman.conversation.api.bailian.webrtc.dto.ExchangeBailianWebRtcResponse;
import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianWebRtcSignalingService;
import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import con.aidenjia.digitalhuman.conversation.config.ResourceServerSecurityConfig;
import con.aidenjia.digitalhuman.conversation.config.WebSecurityConfig;
import con.aidenjia.digitalhuman.conversation.exception.GlobalExceptionHandler;
import con.aidenjia.digitalhuman.conversation.security.CurrentUserIdAccessor;
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

@WebMvcTest(BailianRealtimeWebRtcController.class)
@Import({
    WebSecurityConfig.class,
    ResourceServerSecurityConfig.class,
    GlobalExceptionHandler.class,
    ApiResponseWriter.class,
    CurrentUserIdAccessor.class
})
@EnableConfigurationProperties(ApiSecurityProperties.class)
@TestPropertySource(
        properties = {
            "api.security.issuer-uri=https://test-issuer/",
            "api.security.jwk-set-uri=https://test-issuer/.well-known/jwks",
            "api.security.demo-token-enabled=false"
        })
class BailianRealtimeWebRtcControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private BailianWebRtcSignalingService signalingService;

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
    @DisplayName("webrtc 信令：无 Authorization 返回 401")
    void missingAuthorization() throws Exception {
        mockMvc.perform(
                        post("/api/bailian/realtime/webrtc")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"offerSdp\":\"v=0\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("webrtc 信令：无效 Bearer 返回 4010")
    void invalidBearer() throws Exception {
        mockMvc.perform(
                        post("/api/bailian/realtime/webrtc")
                                .header("Authorization", "Bearer bad")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"offerSdp\":\"v=0\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4010));
    }

    @Test
    @DisplayName("webrtc 信令：只有 offerSdp 也可成功，响应不含声网字段")
    void offerOnlySucceedsWithoutAgoraFields() throws Exception {
        when(signalingService.exchange(eq("user-1"), eq("v=0"), isNull()))
                .thenReturn(
                        new ExchangeBailianWebRtcResponse(
                                "v=answer", "relay-1", BailianRealtimeModels.FLASH));

        mockMvc.perform(
                        post("/api/bailian/realtime/webrtc")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"offerSdp\":\"v=0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answerSdp").value("v=answer"))
                .andExpect(jsonPath("$.data.relayId").value("relay-1"))
                .andExpect(jsonPath("$.data.model").value(BailianRealtimeModels.FLASH))
                .andExpect(jsonPath("$.data.appId").doesNotExist())
                .andExpect(jsonPath("$.data.channel").doesNotExist())
                .andExpect(jsonPath("$.data.token").doesNotExist());

        verify(signalingService).exchange("user-1", "v=0", null);
    }
}
