// mg.lagrace.api.exceptions.BusinessException.java
package mg.lagrace.api.exceptions;

public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}