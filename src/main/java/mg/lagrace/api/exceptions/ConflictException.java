package mg.lagrace.api.exceptions;

import java.time.LocalDate;

public class ConflictException extends BusinessException {
    private final LocalDate conflictFrom;
    private final LocalDate conflictTo;
    private final String field;
    private final java.math.BigDecimal newTotal;

    public ConflictException(String code, String message) {
        super(code, message);
        this.conflictFrom = null;
        this.conflictTo = null;
        this.field = null;
        this.newTotal = null;
    }

    public ConflictException(String code, String message, LocalDate conflictFrom, LocalDate conflictTo) {
        super(code, message);
        this.conflictFrom = conflictFrom;
        this.conflictTo = conflictTo;
        this.field = null;
        this.newTotal = null;
    }

    public ConflictException(String code, String message, String field, java.math.BigDecimal newTotal) {
        super(code, message);
        this.conflictFrom = null;
        this.conflictTo = null;
        this.field = field;
        this.newTotal = newTotal;
    }

    public LocalDate getConflictFrom() { return conflictFrom; }
    public LocalDate getConflictTo() { return conflictTo; }
    public String getField() { return field; }
    public java.math.BigDecimal getNewTotal() { return newTotal; }
}