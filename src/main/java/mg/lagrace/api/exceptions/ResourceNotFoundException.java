// mg.lagrace.api.exceptions.ResourceNotFoundException.java
package mg.lagrace.api.exceptions;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}