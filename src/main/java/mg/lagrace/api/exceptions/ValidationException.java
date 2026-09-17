// mg.lagrace.api.exceptions.ValidationException.java
package mg.lagrace.api.exceptions;

public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}