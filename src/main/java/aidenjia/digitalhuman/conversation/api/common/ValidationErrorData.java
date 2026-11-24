package con.aidenjia.digitalhuman.conversation.api.common;

import java.util.Map;

/** 校验失败时 {@link ApiResponse#error()} 载荷。 */
public record ValidationErrorData(Map<String, String> errors) {}
