package con.aidenjia.digitalhuman.conversation.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import con.aidenjia.digitalhuman.conversation.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentUserIdAccessorTest {

    private final CurrentUserIdAccessor accessor = new CurrentUserIdAccessor();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("无认证时拒绝")
    void rejectsMissingAuthentication() {
        SecurityContextHolder.clearContext();
        ApiException ex = assertThrows(ApiException.class, accessor::requireUserId);
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("非 JWT principal 时拒绝")
    void rejectsNonJwtPrincipal() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "n/a"));
        ApiException ex = assertThrows(ApiException.class, accessor::requireUserId);
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("JWT sub 空白时拒绝")
    void rejectsBlankSubject() {
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .subject("  ")
                        .claim("iss", "https://example/")
                        .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        ApiException ex = assertThrows(ApiException.class, accessor::requireUserId);
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("读取 JWT sub")
    void returnsSubject() {
        Jwt jwt =
                Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .subject("user-42")
                        .claim("iss", "https://example/")
                        .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        assertEquals("user-42", accessor.requireUserId());
    }
}
