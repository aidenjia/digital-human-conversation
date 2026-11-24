package con.aidenjia.digitalhuman.conversation.config;

import con.aidenjia.digitalhuman.conversation.bailian.webrtc.InMemoryBailianWebRtcTicketStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** WebRTC 单进程演示票据存储。 */
@Configuration
public class BailianWebRtcTicketConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(BailianWebRtcTicketConfiguration.class);

    @Bean
    InMemoryBailianWebRtcTicketStore bailianWebRtcTicketMemoryStore() {
        log.info("Bailian WebRTC ticket store=memory (single process)");
        return new InMemoryBailianWebRtcTicketStore();
    }
}
