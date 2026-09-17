// mg.lagrace.api.exceptions.DuplicateResourceException.java
package mg.lagrace.api.exceptions;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", message);
    }
}