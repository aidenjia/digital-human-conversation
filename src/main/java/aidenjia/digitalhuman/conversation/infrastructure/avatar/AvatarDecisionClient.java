package con.aidenjia.digitalhuman.conversation.infrastructure.avatar;

import con.aidenjia.digitalhuman.conversation.avatar.AvatarDecision;
import con.aidenjia.digitalhuman.conversation.avatar.AvatarDecisionInput;
import java.util.concurrent.CompletableFuture;

public interface AvatarDecisionClient {
    boolean isEnabled();

    CompletableFuture<AvatarDecision> decide(AvatarDecisionInput input);
}
