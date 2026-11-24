package con.aidenjia.digitalhuman.conversation.trtc.support;

import con.aidenjia.digitalhuman.conversation.exception.ApiException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Fail-fast per-user join exclusion for one JVM; not success-response replay. */
@Component
public class ConvoaiJoinGuard {
    private final Set<String> joiningUsers = ConcurrentHashMap.newKeySet();

    public <T> T execute(String userId, Supplier<T> join) {
        if (!joiningUsers.add(userId)) {
            throw new ApiException(409, "Request already in progress, please retry later");
        }
        try {
            return join.get();
        } finally {
            joiningUsers.remove(userId);
        }
    }
}
