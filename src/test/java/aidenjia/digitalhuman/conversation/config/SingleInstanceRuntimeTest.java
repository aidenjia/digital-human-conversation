package con.aidenjia.digitalhuman.conversation.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SingleInstanceRuntimeTest {
    @Test
    void distributedInfrastructureIsNotOnTheRuntimeClasspath() {
        for (String name :
                new String[] {"org.redisson.Redisson", "com.baomidou.lock.annotation.Lock4j"}) {
            assertThatThrownBy(() -> Class.forName(name))
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }
}
