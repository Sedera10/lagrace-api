package mg.lagrace.api.exceptions;

import java.sql.SQLException;
import mg.lagrace.api.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> accessDenied(AccessDeniedException ex) { return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "Vous n'avez pas la permission d'effectuer cette action."); }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> stale(ObjectOptimisticLockingFailureException ex) { return response(HttpStatus.CONFLICT, "STALE_VERSION", "Le dossier a été modifié par quelqu'un d'autre."); }
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> validation(ValidationException ex) {
        ApiResponse<Void> body = ApiResponse.apiError("VALIDATION", ex.getMessage());
        body.setField(ex.getField());
        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> beanValidation(MethodArgumentNotValidException ex) {
        var field = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        return field == null ? response(HttpStatus.BAD_REQUEST, "VALIDATION", "Erreur de validation.")
                : ResponseEntity.badRequest().body(ApiResponse.apiError("VALIDATION", field.getDefaultMessage(), field.getField()));
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException ex) { return response(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage()); }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException ex) { return response("FORBIDDEN".equals(ex.getCode()) ? HttpStatus.FORBIDDEN : HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage()); }
    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ApiResponse<Void>> unprocessable(UnprocessableEntityException ex) { return response(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage()); }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(ConflictException ex) {
        ApiResponse<Void> body = ApiResponse.apiError(ex.getCode(), ex.getMessage());
        if (ex.getConflictFrom() != null && ex.getConflictTo() != null)
            body.setConflict(new ApiResponse.ConflictPeriod(ex.getConflictFrom().toString(), ex.getConflictTo().toString()));
        body.setField(ex.getField());
        body.setNewTotal(ex.getNewTotal());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> duplicate(DuplicateResourceException ex) { return response(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> dataIntegrity(DataIntegrityViolationException ex) {
        if (hasSqlState(ex, "23P01")) return response(HttpStatus.CONFLICT, "ROOM_UNAVAILABLE", "Cette chambre est déjà réservée pour cette période.");
        if (hasSqlState(ex, "23505")) return response(HttpStatus.CONFLICT, "UNIQUE_CONFLICT", "Une réservation concurrente utilise déjà cette ressource.");
        return response(HttpStatus.CONFLICT, "UNIQUE_CONFLICT", "Violation de contrainte en base de données.");
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> illegal(IllegalArgumentException ex) { return response(HttpStatus.BAD_REQUEST, "VALIDATION", ex.getMessage()); }
    private boolean hasSqlState(Throwable error, String expected) { for (Throwable cause = error; cause != null; cause = cause.getCause()) if (cause instanceof SQLException sql && expected.equals(sql.getSQLState())) return true; return false; }
    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String code, String message) { return ResponseEntity.status(status).body(ApiResponse.apiError(code, message)); }
}
