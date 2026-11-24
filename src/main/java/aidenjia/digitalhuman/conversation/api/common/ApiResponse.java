package con.aidenjia.digitalhuman.conversation.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应信封：{@code success == true} 且 {@code code == 200} 表示成功。
 *
 * @param <T> 业务数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, boolean success, String message, T data, Object error) {

    public static final int SUCCESS_CODE = 200;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, true, null, data, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, false, message, null, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, T data) {
        return new ApiResponse<>(code, false, message, data, null);
    }

    public static <T> ApiResponse<T> failWithError(int code, String message, Object error) {
        return new ApiResponse<>(code, false, message, null, error);
    }
}
