package con.aidenjia.digitalhuman.conversation.exception;

/**
 * 业务 API 异常。
 *
 * <p>{@link #getCode()} 为响应体业务码；{@link #getHttpStatus()} 为 HTTP 状态。多数场景两者相同；鉴权等独立业务码（如 4010 /
 * 4030）时可分离。
 */
public class ApiException extends RuntimeException {

    private final int code;
    private final int httpStatus;
    private final Object error;

    public ApiException(int code, String message) {
        this(code, code, message, null, null);
    }

    public ApiException(int code, String message, Throwable cause) {
        this(code, code, message, null, cause);
    }

    public ApiException(int code, int httpStatus, String message) {
        this(code, httpStatus, message, null, null);
    }

    public ApiException(int code, int httpStatus, String message, Object error) {
        this(code, httpStatus, message, error, null);
    }

    public ApiException(int code, int httpStatus, String message, Object error, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
        this.error = error;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public Object getError() {
        return error;
    }
}
