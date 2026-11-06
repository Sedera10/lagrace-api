// mg.lagrace.api.exceptions.ValidationException.java
package mg.lagrace.api.exceptions;

public class ValidationException extends BusinessException {
    private final String field;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.field = null;
    }

    public ValidationException(String message, String field) {
        super("VALIDATION", message);
        this.field = field;
    }

    public String getField() { return field; }
}