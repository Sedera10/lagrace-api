package mg.lagrace.api.models;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_change_log")
public class BookingChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_change")
    private Long id;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;
    @Column(name = "object_type", nullable = false, length = 15)
    private String objectType;
    @Column(name = "object_id", nullable = false)
    private Long objectId;
    @Column(name = "action", nullable = false, length = 10)
    private String action;
    @Column(name = "field_name", length = 50)
    private String fieldName;
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    @Column(name = "reason", length = 255)
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    @PrePersist
    void onCreate() { if (changedAt == null) changedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public UUID getOperationId() { return operationId; }
    public void setOperationId(UUID value) { operationId = value; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long value) { bookingId = value; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String value) { objectType = value; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long value) { objectId = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String value) { fieldName = value; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String value) { oldValue = value; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String value) { newValue = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public User getChangedByUser() { return changedByUser; }
    public void setChangedByUser(User value) { changedByUser = value; }
    public OffsetDateTime getChangedAt() { return changedAt; }
}
