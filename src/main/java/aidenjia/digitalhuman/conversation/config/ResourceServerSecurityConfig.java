package con.aidenjia.digitalhuman.conversation.config;

import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import con.aidenjia.digitalhuman.conversation.security.AuthFailureResponses;
import con.aidenjia.digitalhuman.conversation.security.BearerTokenSupport;
import con.aidenjia.digitalhuman.conversation.security.DemoAwareBearerTokenResolver;
import con.aidenjia.digitalhuman.conversation.security.DemoBearerAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class ResourceServerSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ResourceServerSecurityConfig.class);

    private final ApiSecurityProperties apiSecurity;
    private final ApiResponseWriter apiResponseWriter;

    public ResourceServerSecurityConfig(
            ApiSecurityProperties apiSecurity, ApiResponseWriter apiResponseWriter) {
        this.apiSecurity = apiSecurity;
        this.apiResponseWriter = apiResponseWriter;
    }

    @Bean
    SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        // /api/** 默认需 JWT；仅下列公开例外。新增业务接口勿再逐条登记。
        // /ws/bailian/realtime-events 走 anyRequest().permitAll()，鉴权在 HandshakeInterceptor（JWT 或
        // demo-token）。
        // DemoBearerAuthenticationFilter：agents + POST /api/rtc/credentials + POST
        // /api/bailian/realtime/webrtc。
        http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/",
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/actuator/prometheus")
                                        .permitAll()
                                        .requestMatchers("/api/**")
                                        .authenticated()
                                        .anyRequest()
                                        .permitAll())
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.bearerTokenResolver(new DemoAwareBearerTokenResolver())
                                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                                        .authenticationEntryPoint(authenticationEntryPoint()))
                .addFilterBefore(
                        new DemoBearerAuthenticationFilter(apiSecurity),
                        BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = buildDecoder();
        OAuth2TokenValidator<Jwt> timestampValidator =
                new JwtTimestampValidator(Duration.ofSeconds(apiSecurity.getClockSkewSeconds()));
        OAuth2TokenValidator<Jwt> issuerValidator =
                validIssuersValidator(apiSecurity.resolveValidIssuers());
        decoder.setJwtValidator(new DelegatingJwtValidators(timestampValidator, issuerValidator));
        return decoder;
    }

    private NimbusJwtDecoder buildDecoder() {
        // 允许 typ=at+jwt 的访问令牌，签名、时间与 issuer 仍独立校验。
        if (StringUtils.hasText(apiSecurity.getJwkSetUri())) {
            return NimbusJwtDecoder.withJwkSetUri(apiSecurity.getJwkSetUri().trim())
                    .validateType(false)
                    .build();
        }
        return NimbusJwtDecoder.withIssuerLocation(apiSecurity.getIssuerUri().trim())
                .validateType(false)
                .build();
    }

    static OAuth2TokenValidator<Jwt> validIssuersValidator(List<String> validIssuers) {
        List<String> normalizedAllowed =
                validIssuers.stream()
                        .filter(StringUtils::hasText)
                        .map(ResourceServerSecurityConfig::normalizeIssuer)
                        .toList();
        return jwt -> {
            if (normalizedAllowed.isEmpty()) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Issuer validator not configured", null));
            }
            String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
            if (iss != null && normalizedAllowed.contains(normalizeIssuer(iss))) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid issuer", null));
        };
    }

    /** 忽略尾部 {@code /} 差异，避免 JWT {@code iss} 与配置逐字节不一致导致恒 4010。 */
    static String normalizeIssuer(String issuer) {
        String trimmed = issuer.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            Throwable cause =
                    authException.getCause() != null ? authException.getCause() : authException;
            log.warn(
                    "JWT auth failed path={} message={}",
                    request.getRequestURI(),
                    cause.getMessage());
            int code = resolveFailureCode(request);
            AuthFailureResponses.write(request, response, apiResponseWriter, code);
        };
    }

    static int resolveFailureCode(HttpServletRequest request) {
        if (BearerTokenSupport.isMalformedBearerRequest(
                request.getHeader(BearerTokenSupport.authorizationHeaderName()))) {
            return AuthFailureResponses.CODE_UNAUTHENTICATED;
        }
        return AuthFailureResponses.CODE_INVALID_TOKEN;
    }

    /** 组合多个 JWT validator（与 Spring {@code DelegatingOAuth2TokenValidator} 等价，避免额外 import）。 */
    static final class DelegatingJwtValidators implements OAuth2TokenValidator<Jwt> {

        private final OAuth2TokenValidator<Jwt>[] validators;

        @SafeVarargs
        DelegatingJwtValidators(OAuth2TokenValidator<Jwt>... validators) {
            this.validators = validators;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            OAuth2TokenValidatorResult result = OAuth2TokenValidatorResult.success();
            for (OAuth2TokenValidator<Jwt> validator : validators) {
                result = validator.validate(token);
                if (result.hasErrors()) {
                    return result;
                }
            }
            return result;
        }
    }
}
