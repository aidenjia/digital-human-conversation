package con.aidenjia.digitalhuman.conversation.memory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/** Thread-safe process-local state with expiry and atomic conditional consumption. */
public final class ExpiringMemoryStore<V> {
    private record Entry<V>(V value, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry<V>> entries = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    public ExpiringMemoryStore(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    public void put(String key, V value) {
        entries.put(key, new Entry<>(value, clock.instant().plus(ttl)));
    }

    public Optional<V> get(String key) {
        if (key == null) return Optional.empty();
        Entry<V> entry =
                entries.computeIfPresent(key, (id, current) -> expired(current) ? null : current);
        return entry == null ? Optional.empty() : Optional.of(entry.value());
    }

    public Optional<V> consume(String key, Predicate<V> allowed) {
        if (key == null) return Optional.empty();
        AtomicReference<V> result = new AtomicReference<>();
        entries.computeIfPresent(
                key,
                (id, entry) -> {
                    if (expired(entry)) return null;
                    if (!allowed.test(entry.value())) return entry;
                    result.set(entry.value());
                    return null;
                });
        return Optional.ofNullable(result.get());
    }

    public void remove(String key) {
        if (key != null) entries.remove(key);
    }

    public void removeExpired() {
        entries.forEach(
                (key, entry) -> {
                    if (expired(entry)) entries.remove(key, entry);
                });
    }

    private boolean expired(Entry<V> entry) {
        return !clock.instant().isBefore(entry.expiresAt());
    }
}
