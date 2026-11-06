package mg.lagrace.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String status;
    private T data;
    private String error;
    private String code;
    private String field;
    private String message;
    private ConflictPeriod conflict;
    private java.math.BigDecimal newTotal;
    private Long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(String status, T data, String error, String message) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResponse<T> apiError(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>("error", null, code, message);
        response.code = code;
        return response;
    }

    public static <T> ApiResponse<T> apiError(String code, String message, String field) {
        ApiResponse<T> response = apiError(code, message);
        response.field = field;
        return response;
    }

    // Méthodes statiques pour faciliter la création
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", data, null, message);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>("error", null, error, null);
    }

    public static <T> ApiResponse<T> error(String error, String message) {
        return new ApiResponse<>("error", null, error, message);
    }

    // Getters et Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getCode() { return code == null ? error : code; }
    public void setCode(String code) { this.code = code; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public ConflictPeriod getConflict() { return conflict; }
    public void setConflict(ConflictPeriod conflict) { this.conflict = conflict; }
    public java.math.BigDecimal getNewTotal() { return newTotal; }
    public void setNewTotal(java.math.BigDecimal newTotal) { this.newTotal = newTotal; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public record ConflictPeriod(String from, String to) {}
}
