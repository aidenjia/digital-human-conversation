package con.aidenjia.digitalhuman.conversation.trtc.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import con.aidenjia.digitalhuman.conversation.exception.ApiException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConvoaiJoinGuardTest {
    @Test
    void rejectsSameUserButAllowsOtherUsersAndReleasesAfterCompletion() throws Exception {
        var guard = new ConvoaiJoinGuard();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var request =
                    pool.submit(
                            () ->
                                    guard.execute(
                                            "user",
                                            () -> {
                                                entered.countDown();
                                                try {
                                                    assertThat(release.await(5, TimeUnit.SECONDS))
                                                            .isTrue();
                                                } catch (InterruptedException ex) {
                                                    Thread.currentThread().interrupt();
                                                    throw new RuntimeException(ex);
                                                }
                                                return "first";
                                            }));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> guard.execute("user", () -> "duplicate"))
                        .isInstanceOfSatisfying(
                                ApiException.class,
                                ex -> assertThat(ex.getHttpStatus()).isEqualTo(409));
                assertThat(guard.execute("other", () -> "other")).isEqualTo("other");
            } finally {
                release.countDown();
            }
            assertThat(request.get(5, TimeUnit.SECONDS)).isEqualTo("first");
        }
        assertThat(guard.execute("user", () -> "again")).isEqualTo("again");
    }

    @Test
    void releasesUserWhenJoinFails() {
        var guard = new ConvoaiJoinGuard();
        assertThatThrownBy(
                        () ->
                                guard.execute(
                                        "user",
                                        () -> {
                                            throw new IllegalStateException("upstream");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(guard.execute("user", () -> "retry")).isEqualTo("retry");
    }
}
