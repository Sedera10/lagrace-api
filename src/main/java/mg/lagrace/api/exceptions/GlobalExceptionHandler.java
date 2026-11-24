package mg.lagrace.api.exceptions;

import java.sql.SQLException;
import mg.lagrace.api.shared.api.ApiResponse;
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
    public ResponseEntity<ApiResponse<Void>> business(BusinessException ex) {
        ApiResponse<Void> body = ApiResponse.apiError(ex.getCode(), ex.getMessage());
        body.setField(ex.getField());
        return ResponseEntity.status("FORBIDDEN".equals(ex.getCode()) ? HttpStatus.FORBIDDEN : HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }
    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ApiResponse<Void>> unprocessable(UnprocessableEntityException ex) {
        ApiResponse<Void> body = ApiResponse.apiError(ex.getCode(), ex.getMessage());
        body.setField(ex.getField());
        return ResponseEntity.unprocessableEntity().body(body);
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> conflict(ConflictException ex) {
        ApiResponse<Object> body = ApiResponse.apiError(ex.getCode(), ex.getMessage());
        if (ex.getConflictFrom() != null && ex.getConflictTo() != null)
            body.setConflict(new ApiResponse.ConflictPeriod(ex.getConflictFrom().toString(), ex.getConflictTo().toString()));
        body.setField(ex.getField());
        body.setNewTotal(ex.getNewTotal());
        body.setData(ex.getData());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> duplicate(DuplicateResourceException ex) { return response(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> dataIntegrity(DataIntegrityViolationException ex) {
        String sqlState = sqlState(ex);
        String constraint = constraintName(ex);
        if ("uq_customer_document".equals(constraint))
            return response(HttpStatus.CONFLICT, "DUPLICATE_DOCUMENT", "Cette pièce d’identité est déjà associée à un client.");
        if ("no_overlap_leave".equals(constraint))
            return response(HttpStatus.CONFLICT, "LEAVE_OVERLAP", "Ce congé chevauche un congé existant.");
        if ("23P01".equals(sqlState) || "no_double_room".equals(constraint))
            return response(HttpStatus.CONFLICT, "ROOM_UNAVAILABLE", "Une autre réservation chevauche la période demandée pour cette chambre.");
        if ("uq_space_day".equals(constraint))
            return response(HttpStatus.CONFLICT, "SPACE_UNAVAILABLE", "Cet espace est déjà réservé à cette date.");
        if ("uq_invoice_line_source".equals(constraint))
            return response(HttpStatus.CONFLICT, "ALREADY_BILLED", "Cet élément a déjà été facturé.");
        if ("uq_invoice_line_credited".equals(constraint))
            return response(HttpStatus.CONFLICT, "CREDIT_NOT_ALLOWED", "Cette ligne a déjà fait l'objet d'un avoir.");
        if ("23505".equals(sqlState))
            return response(HttpStatus.CONFLICT, "UNIQUE_CONFLICT", "Une référence existe déjà. Actualisez le dossier puis réessayez.");
        if ("23503".equals(sqlState))
            return response(HttpStatus.CONFLICT, "INVALID_REFERENCE", "La chambre, le dossier ou le statut associé n'existe plus. Actualisez puis réessayez.");
        if ("23514".equals(sqlState))
            return response(HttpStatus.CONFLICT, "INVALID_DATA", "Les dates ou les valeurs saisies ne respectent pas les contraintes de réservation.");
        if ("23502".equals(sqlState))
            return response(HttpStatus.CONFLICT, "MISSING_DATA", "Une donnée obligatoire manque pour enregistrer cette réservation.");
        String message = constraint == null
                ? "Une contrainte a refusé cette opération. Actualisez le dossier puis réessayez."
                : "La contrainte " + constraint + " a refusé cette opération. Actualisez le dossier puis réessayez.";
        return response(HttpStatus.CONFLICT, "DATABASE_CONSTRAINT", message);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> illegal(IllegalArgumentException ex) { return response(HttpStatus.BAD_REQUEST, "VALIDATION", ex.getMessage()); }
    private String sqlState(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) return sql.getSQLState();
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && violation.getSQLException() != null)
                return violation.getSQLException().getSQLState();
        }
        return null;
    }
    private String constraintName(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause())
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation)
                return violation.getConstraintName();
        return null;
    }
    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String code, String message) { return ResponseEntity.status(status).body(ApiResponse.apiError(code, message)); }
}
