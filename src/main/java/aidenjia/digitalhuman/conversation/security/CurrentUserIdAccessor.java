package con.aidenjia.digitalhuman.conversation.security;

import con.aidenjia.digitalhuman.conversation.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** 从 Resource Server JWT 读取 {@code sub} 作为业务 {@code user_id}。 */
@Component
public class CurrentUserIdAccessor {

    public String requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(401, "Cannot authenticate");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new ApiException(401, "Cannot authenticate");
        }
        return subject.trim();
    }
}
