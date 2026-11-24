package con.aidenjia.digitalhuman.conversation.trtc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoomIdGeneratorTest {

    @Test
    @DisplayName("生成 32 位无连字符十六进制 UUID")
    void generatesUuidWithoutHyphens() {
        String id = RoomIdGenerator.generate();
        assertEquals(RoomIdGenerator.ROOM_ID_LENGTH, id.length());
        assertTrue(id.matches("[0-9a-f]{32}"));
    }

    @Test
    @DisplayName("1 秒内生成 1 万个互不相同的 roomId")
    void generatesTenThousandUniqueIdsWithinOneSecond() {
        Set<String> ids = new HashSet<>(10_000);
        long startNanos = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            ids.add(RoomIdGenerator.generate());
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        assertEquals(10_000, ids.size(), "存在重复的 roomId");
        assertTrue(elapsedMs < 1_000, "生成 1 万个 roomId 耗时 " + elapsedMs + "ms，超过 1 秒");
    }
}
