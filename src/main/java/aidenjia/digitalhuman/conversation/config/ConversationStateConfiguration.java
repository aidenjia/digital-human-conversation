package con.aidenjia.digitalhuman.conversation.config;

import con.aidenjia.digitalhuman.conversation.trtc.support.InMemoryConvoaiAgentOwnershipStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** This demo supports exactly one backend process. No external state store is required. */
@Configuration
public class ConversationStateConfiguration {
    @Bean
    InMemoryConvoaiAgentOwnershipStore convoaiAgentOwnershipStore(ConvoaiProperties properties) {
        return new InMemoryConvoaiAgentOwnershipStore(
                properties.getAgentOwnershipTtlSeconds(), Clock.systemUTC());
    }
}
