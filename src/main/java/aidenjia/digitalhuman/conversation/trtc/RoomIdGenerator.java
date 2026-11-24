package con.aidenjia.digitalhuman.conversation.trtc;

import java.util.UUID;

/** 生成字符串房间号 / SessionId：无连字符的 32 位十六进制 UUID。 */
public final class RoomIdGenerator {

    static final int ROOM_ID_LENGTH = 32;

    private RoomIdGenerator() {}

    /**
     * @return 固定 32 字符、小写十六进制、无连字符
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
