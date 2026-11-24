package con.aidenjia.digitalhuman.conversation;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import con.aidenjia.digitalhuman.conversation.api.common.ApiVersionConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(WebApiFoundationIT.ContractFixture.class)
class WebApiFoundationIT {

    @Autowired private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @org.junit.jupiter.api.BeforeEach
    void authorizeFixtureRequests() {
        org.mockito.Mockito.when(jwtDecoder.decode("valid-token"))
                .thenReturn(
                        org.springframework.security.oauth2.jwt.Jwt.withTokenValue("valid-token")
                                .header("alg", "RS256")
                                .subject("foundation-user")
                                .build());
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @org.springframework.web.bind.annotation.RestController
    static class ContractFixture {
        @org.springframework.web.bind.annotation.PostMapping("/api/test-contract")
        con.aidenjia.digitalhuman.conversation.api.common.ApiResponse<java.util.Map<String, String>>
                v1(
                        @jakarta.validation.Valid
                                @org.springframework.web.bind.annotation.RequestBody
                                Name body) {
            return con.aidenjia.digitalhuman.conversation.api.common.ApiResponse.ok(
                    java.util.Map.of("message", "hello, " + body.name()));
        }

        record Name(@jakarta.validation.constraints.NotBlank String name) {}
    }

    @Test
    @DisplayName("GET / 根探活可被访问且返回应用标识")
    void rootReturnsNameAndVersion() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("digital-human-conversation"))
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    @DisplayName("POST /api/test-contract 合法 body 返回 200 与业务消息")
    void echoValidBodyReturnsOk() throws Exception {
        mockMvc.perform(
                        post("/api/test-contract")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"world\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("hello, world"))
                .andExpect(header().string("Content-Language", "en"))
                .andExpect(header().doesNotExist(ApiVersionConstants.RESPONSE_SUPPORTED_HEADER));
    }

    @Test
    void secondVersionIsRejectedForSingleVersionEndpoint() throws Exception {
        mockMvc.perform(
                        post("/api/test-contract")
                                .header("Authorization", "Bearer valid-token")
                                .header(
                                        ApiVersionConstants.REQUEST_HEADER,
                                        ApiVersionConstants.VERSION_2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"world\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("not available")));
    }

    @Test
    @DisplayName("POST /api/test-contract 未知 X-Api-Version 返回 400")
    void echoUnsupportedVersionReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/test-contract")
                                .header("Authorization", "Bearer valid-token")
                                .header(ApiVersionConstants.REQUEST_HEADER, "99")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"world\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Unsupported API version")));
    }

    @Test
    @DisplayName("POST /api/test-contract 非法 body 返回 400 ApiResponse")
    void invalidEchoBodyReturns400ApiResponse() throws Exception {
        mockMvc.perform(
                        post("/api/test-contract")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Validation")))
                .andExpect(jsonPath("$.error.errors").isMap());
    }

    @Test
    @DisplayName("业务接口在未加 /api 前缀时不暴露")
    void echoNotExposedWithoutApiPrefix() throws Exception {
        mockMvc.perform(
                        post("/echo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /actuator/health 在站点根路径可访问（无前缀）")
    void actuatorHealthExposedAtRoot() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/prometheus 在站点根路径可访问（无前缀）")
    void actuatorPrometheusExposedAtRoot() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "application=\"digital-human-conversation\"")))
                .andExpect(content().string(containsString("env=\"test\"")))
                .andExpect(content().string(containsString("http_server_requests")));
    }

    @Test
    @DisplayName("GET /api/internal/active-profiles 非公开例外，无 JWT 拒绝（默认 /api/**）")
    void internalActiveProfilesNotUnderApiPrefix() throws Exception {
        // 真实内部接口在 /internal/...；误挂到 /api 下亦须鉴权，不得匿名 404 旁路
        mockMvc.perform(get("/api/internal/active-profiles")).andExpect(status().isUnauthorized());
    }
}
