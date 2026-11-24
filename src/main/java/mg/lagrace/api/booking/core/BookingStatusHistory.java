package mg.lagrace.api.booking.core;

import jakarta.persistence.*;
import mg.lagrace.api.auth.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "booking_status_history")
public class BookingStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_history")
    private Long idHistory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "line_type", length = 15)
    private String lineType;

    @Column(name = "line_ref")
    private String lineId;

    @Column(name = "old_status", length = 20)
    private String oldStatusCode;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatusCode;

    @Column(name = "reason", length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) changedAt = OffsetDateTime.now();
    }

    public Long getIdHistory() { return idHistory; }
    public void setIdHistory(Long idHistory) { this.idHistory = idHistory; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public String getLineType() { return lineType; }
    public void setLineType(String lineType) { this.lineType = lineType; }
    public String getLineId() { return lineId; }
    public void setLineId(String lineId) { this.lineId = lineId; }
    public String getOldStatusCode() { return oldStatusCode; }
    public void setOldStatusCode(String oldStatusCode) { this.oldStatusCode = oldStatusCode; }
    public String getNewStatusCode() { return newStatusCode; }
    public void setNewStatusCode(String newStatusCode) { this.newStatusCode = newStatusCode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public User getChangedByUser() { return changedByUser; }
    public void setChangedByUser(User changedByUser) { this.changedByUser = changedByUser; }
    public OffsetDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }
}
