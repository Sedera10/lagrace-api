// mg.lagrace.api.exceptions.BusinessException.java
package mg.lagrace.api.exceptions;

public class BusinessException extends RuntimeException {
    private final String code;
    private final String field;

    public BusinessException(String code, String message) {
        this(code, message, null);
    }

    public BusinessException(String code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() { return code; }
    public String getField() { return field; }
}