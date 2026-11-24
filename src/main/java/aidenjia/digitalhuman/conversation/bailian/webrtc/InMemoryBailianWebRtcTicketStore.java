package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import con.aidenjia.digitalhuman.conversation.memory.ExpiringMemoryStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;

/** Single-process tickets with a sixty-second TTL and atomic, owner-bound consumption. */
public final class InMemoryBailianWebRtcTicketStore implements BailianWebRtcTicketStore {

    private final ExpiringMemoryStore<BailianWebRtcTicket> tickets;

    public InMemoryBailianWebRtcTicketStore() {
        this(Clock.systemUTC());
    }

    public InMemoryBailianWebRtcTicketStore(Clock clock) {
        tickets = new ExpiringMemoryStore<>(Duration.ofSeconds(60), clock);
    }

    @Override
    public void save(String relayId, String userId, String model) {
        tickets.put(relayId, new BailianWebRtcTicket(userId, model));
    }

    @Override
    public Optional<BailianWebRtcTicket> find(String relayId) {
        return tickets.get(relayId);
    }

    @Override
    public void delete(String relayId) {
        tickets.remove(relayId);
    }

    @Override
    public Optional<BailianWebRtcTicket> consume(String relayId, String userId) {
        return tickets.consume(relayId, ticket -> ticket.userId().equals(userId));
    }

    @Scheduled(fixedDelay = 60000)
    public void removeExpired() {
        tickets.removeExpired();
    }
}
