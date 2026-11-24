package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InMemoryBailianWebRtcTicketStoreTest {
    @Test
    void expiresExactlyAfterSixtySecondsAndCleansUnusedTickets() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-10-30T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        var store = new InMemoryBailianWebRtcTicketStore(clock);
        store.save("ticket", "owner", "model");
        when(clock.instant()).thenReturn(start.plusSeconds(59));
        assertThat(store.find("ticket")).isPresent();
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        assertThat(store.consume("ticket", "owner")).isEmpty();
        assertThat(store.find("ticket")).isEmpty();
        store.save("unused", "owner", "model");
        when(clock.instant()).thenReturn(start.plusSeconds(120));
        store.removeExpired();
        when(clock.instant()).thenReturn(start);
        assertThat(store.find("unused")).isEmpty();
    }

    @Test
    void foreignUserCannotConsumeAndOwnerCanOnlyConsumeOnce() {
        var store = new InMemoryBailianWebRtcTicketStore();
        store.save("ticket", "owner", "model");
        assertThat(store.consume("ticket", "other")).isEmpty();
        assertThat(store.consume("ticket", "owner"))
                .contains(new BailianWebRtcTicket("owner", "model"));
        assertThat(store.consume("ticket", "owner")).isEmpty();
    }

    @Test
    void concurrentConsumersHaveExactlyOneWinner() throws Exception {
        var store = new InMemoryBailianWebRtcTicketStore();
        store.save("ticket", "owner", "model");
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first =
                    pool.submit(
                            () -> {
                                start.await();
                                return store.consume("ticket", "owner");
                            });
            var second =
                    pool.submit(
                            () -> {
                                start.await();
                                return store.consume("ticket", "owner");
                            });
            start.countDown();
            assertThat(
                            java.util.stream.Stream.of(
                                            first.get(5, TimeUnit.SECONDS),
                                            second.get(5, TimeUnit.SECONDS))
                                    .filter(java.util.Optional::isPresent)
                                    .count())
                    .isEqualTo(1);
        }
    }
}
