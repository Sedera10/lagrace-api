package mg.lagrace.api.services;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import mg.lagrace.api.models.BookingChangeLog;
import mg.lagrace.api.repositories.BookingChangeLogRepository;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class BookingChangeLogger {
    private final BookingChangeLogRepository repository;
    private final UserRepository users;
        private final ThreadLocal<UUID> operationContext = new ThreadLocal<>();

    public BookingChangeLogger(BookingChangeLogRepository repository, UserRepository users) {
        this.repository = repository;
        this.users = users;
    }

        public UUID beginOperation() {
            UUID operationId = UUID.randomUUID();
            operationContext.set(operationId);
            return operationId;
        }

        public void endOperation() { operationContext.remove(); }

    public void log(Long bookingId, String objectType, Long objectId, String action,
                    String fieldName, Object oldValue, Object newValue, String reason,
                    Long userId, UUID operationId) {
        if (Objects.equals(stringify(oldValue), stringify(newValue))) return;
        BookingChangeLog log = new BookingChangeLog();
        log.setBookingId(bookingId);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setAction(action);
        log.setFieldName(fieldName);
        log.setOldValue(stringify(oldValue));
        log.setNewValue(stringify(newValue));
        log.setReason(reason);
            UUID currentOperation = operationContext.get();
            log.setOperationId(currentOperation == null ? (operationId == null ? UUID.randomUUID() : operationId) : currentOperation);
        if (userId != null) users.findById(userId).ifPresent(log::setChangedByUser);
        repository.save(log);
    }

    private String stringify(Object value) { return value == null ? null : String.valueOf(value); }
}
