package mg.lagrace.api.exceptions;

public class UnprocessableEntityException extends BusinessException {
    public UnprocessableEntityException(String message) {
        super("UNPROCESSABLE_ENTITY", message);
    }

    public UnprocessableEntityException(String code, String message) {
        super(code, message);
    }
}