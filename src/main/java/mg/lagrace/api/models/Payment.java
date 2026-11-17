package mg.lagrace.api.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_payment")
    private Long idPayment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "method_id", nullable = false)
    private PaymentMethod method;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kind_id", nullable = false)
    private PaymentKind kind;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_user_id")
    private User receivedByUser;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "cash_to_hand_back", precision = 12, scale = 2)
    private BigDecimal cashToHandBack;

    @PrePersist
    protected void onCreate() {
        if (paidAt == null) paidAt = OffsetDateTime.now();
    }

    public Long getIdPayment() { return idPayment; }
    public void setIdPayment(Long idPayment) { this.idPayment = idPayment; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public PaymentKind getKind() { return kind; }
    public void setKind(PaymentKind kind) { this.kind = kind; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
    public User getReceivedByUser() { return receivedByUser; }
    public void setReceivedByUser(User receivedByUser) { this.receivedByUser = receivedByUser; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public BigDecimal getCashToHandBack() { return cashToHandBack; }
    public void setCashToHandBack(BigDecimal cashToHandBack) { this.cashToHandBack = cashToHandBack; }
}