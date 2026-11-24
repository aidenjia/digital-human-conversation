package con.aidenjia.digitalhuman.conversation.trtc.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryConvoaiAgentOwnershipStoreTest {
    @Test
    void bindsExpiresAndUnbindsOwners() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-10-30T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        var store = new InMemoryConvoaiAgentOwnershipStore(10, clock);
        store.bind(" agent ", " owner ");
        assertThat(store.findOwner("agent")).contains("owner");
        when(clock.instant()).thenReturn(start.plusSeconds(10));
        assertThat(store.findOwner("agent")).isEmpty();
        store.bind("agent", "owner");
        store.unbind(" agent ");
        assertThat(store.findOwner("agent")).isEmpty();
        assertThat(store.findOwner(null)).isEmpty();
    }

    @Test
    void cleansExpiredEntriesEvenWhenNeverRead() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-10-30T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        var store = new InMemoryConvoaiAgentOwnershipStore(10, clock);
        store.bind("agent", "owner");
        when(clock.instant()).thenReturn(start.plusSeconds(10));
        store.removeExpired();
        when(clock.instant()).thenReturn(start);
        assertThat(store.findOwner("agent")).isEmpty();
    }
}
