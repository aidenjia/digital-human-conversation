package con.aidenjia.digitalhuman.conversation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import con.aidenjia.digitalhuman.conversation.security.AuthFailureResponses;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class ResourceServerSecurityConfigTest {

    @Test
    @DisplayName("缺 Authorization 归 code 401")
    void missingAuthorizationIs401() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        assertThat(ResourceServerSecurityConfig.resolveFailureCode(request))
                .isEqualTo(AuthFailureResponses.CODE_UNAUTHENTICATED);
    }

    @Test
    @DisplayName("非 Bearer 归 code 401")
    void nonBearerIs401() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic abc");
        assertThat(ResourceServerSecurityConfig.resolveFailureCode(request))
                .isEqualTo(AuthFailureResponses.CODE_UNAUTHENTICATED);
    }

    @Test
    @DisplayName("空 Bearer token 归 code 401")
    void emptyBearerTokenIs401() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        assertThat(ResourceServerSecurityConfig.resolveFailureCode(request))
                .isEqualTo(AuthFailureResponses.CODE_UNAUTHENTICATED);
    }

    @Test
    @DisplayName("有 Bearer token 但验签失败归 code 4010")
    void presentBearerTokenIs4010() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.jwt.token");
        assertThat(ResourceServerSecurityConfig.resolveFailureCode(request))
                .isEqualTo(AuthFailureResponses.CODE_INVALID_TOKEN);
    }

    @Test
    @DisplayName("合法 iss 通过 ValidIssuers 校验")
    void validIssuerAccepted() {
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .issuer("https://issuer-prod.example.test/")
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build();
        var validator =
                ResourceServerSecurityConfig.validIssuersValidator(
                        List.of(
                                "https://issuer-prod.example.test/",
                                "https://issuer-stage.example.test/"));
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("iss 无尾斜杠时仍可通过 ValidIssuers 校验")
    void issuerWithoutTrailingSlashAccepted() {
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .issuer("https://issuer-stage.example.test")
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build();
        var validator =
                ResourceServerSecurityConfig.validIssuersValidator(
                        List.of("https://issuer-stage.example.test/"));
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("非法 iss 未通过 ValidIssuers 校验")
    void invalidIssuerRejected() {
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .issuer("https://evil.example/")
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build();
        var validator =
                ResourceServerSecurityConfig.validIssuersValidator(
                        List.of("https://issuer-prod.example.test/"));
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertThat(result.hasErrors()).isTrue();
    }
}
