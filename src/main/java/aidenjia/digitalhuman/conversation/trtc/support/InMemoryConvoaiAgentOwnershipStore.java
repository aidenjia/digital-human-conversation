package con.aidenjia.digitalhuman.conversation.trtc.support;

import con.aidenjia.digitalhuman.conversation.memory.ExpiringMemoryStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

/** Single-process ownership. Restart loses bindings; unknown owners remain fail-closed. */
public final class InMemoryConvoaiAgentOwnershipStore implements ConvoaiAgentOwnershipStore {
    private final ExpiringMemoryStore<String> owners;

    public InMemoryConvoaiAgentOwnershipStore(int ttlSeconds, Clock clock) {
        owners = new ExpiringMemoryStore<>(Duration.ofSeconds(Math.max(1, ttlSeconds)), clock);
    }

    @Override
    public void bind(String agentId, String userId) {
        if (StringUtils.hasText(agentId) && StringUtils.hasText(userId)) {
            owners.put(agentId.trim(), userId.trim());
        }
    }

    @Override
    public Optional<String> findOwner(String agentId) {
        return owners.get(agentId == null ? null : agentId.trim());
    }

    @Override
    public void unbind(String agentId) {
        owners.remove(agentId == null ? null : agentId.trim());
    }

    @Scheduled(fixedDelay = 60000)
    public void removeExpired() {
        owners.removeExpired();
    }
}
