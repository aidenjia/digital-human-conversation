package con.aidenjia.digitalhuman.conversation.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import con.aidenjia.digitalhuman.conversation.api.common.ApiResponse;
import con.aidenjia.digitalhuman.conversation.api.common.ValidationErrorData;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.DigitalHumanType;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Optional<ValidationErrorData> digitalHumanTypeError = digitalHumanTypeValidationError(ex);
        if (digitalHumanTypeError.isPresent()) {
            ApiResponse<Void> body =
                    ApiResponse.failWithError(
                            400, "Validation failed", digitalHumanTypeError.get());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
        ApiResponse<Void> body = ApiResponse.failWithError(400, "Malformed JSON request", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private static Optional<ValidationErrorData> digitalHumanTypeValidationError(
            HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormat
                && DigitalHumanType.class.equals(invalidFormat.getTargetType())
                && invalidFormat.getPath().stream()
                        .anyMatch(
                                reference -> "digitalHumanType".equals(reference.getFieldName()))) {
            return Optional.of(
                    new ValidationErrorData(
                            Map.of("digitalHumanType", DigitalHumanType.INVALID_VALUE_MESSAGE)));
        }
        return Optional.empty();
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> errors = collectFieldErrors(ex);
        ApiResponse<Void> body =
                ApiResponse.failWithError(
                        400, "Validation failed", new ValidationErrorData(errors));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {
        Map<String, String> errors =
                ex.getConstraintViolations().stream()
                        .collect(
                                Collectors.toMap(
                                        v -> v.getPropertyPath().toString(),
                                        v -> v.getMessage(),
                                        (a, b) -> a + "; " + b));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiResponse.failWithError(
                                400, "Validation failed", new ValidationErrorData(errors)));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        if (ex.getHttpStatus() >= 500) {
            log.error(
                    "API error code={} httpStatus={} message={}",
                    ex.getCode(),
                    ex.getHttpStatus(),
                    ex.getMessage(),
                    ex);
        }
        ApiResponse<Void> body =
                ex.getError() == null
                        ? ApiResponse.fail(ex.getCode(), ex.getMessage())
                        : ApiResponse.failWithError(ex.getCode(), ex.getMessage(), ex.getError());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    private static Map<String, String> collectFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(
                        Collectors.toMap(
                                fe -> fe.getField(),
                                fe ->
                                        fe.getDefaultMessage() == null
                                                ? "invalid"
                                                : fe.getDefaultMessage(),
                                (a, b) -> a + "; " + b));
    }
}
