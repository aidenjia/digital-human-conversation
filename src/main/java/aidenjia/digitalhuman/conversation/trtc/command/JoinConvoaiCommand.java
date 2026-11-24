package con.aidenjia.digitalhuman.conversation.trtc.command;

import con.aidenjia.digitalhuman.conversation.api.convoai.dto.DigitalHumanType;

/** Service 入参：join 请求体字段；RTC {@code userId} 取 JWT {@code sub}。 */
public record JoinConvoaiCommand(
        String deviceId, DigitalHumanType digitalHumanType, String timezone) {}
