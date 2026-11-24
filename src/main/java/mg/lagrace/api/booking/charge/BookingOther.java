package mg.lagrace.api.booking.charge;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.booking.core.Booking;

@Entity
@Table(name = "booking_other")
public class BookingOther {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_booking_other")
    private Long idBookingOther;

    @Column(name = "ref", nullable = false, unique = true, length = 20)
    @Generated(event = EventType.INSERT)
    private String ref;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_cancellation_fee", nullable = false)
    private Boolean isCancellationFee = false;

    @Column(name = "is_discount", nullable = false)
    private Boolean isDiscount = false;

    @Column(name = "voided_at")
    private OffsetDateTime voidedAt;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public Long getIdBookingOther() { return idBookingOther; }
    public void setIdBookingOther(Long idBookingOther) { this.idBookingOther = idBookingOther; }
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Boolean getIsCancellationFee() { return isCancellationFee; }
    public void setIsCancellationFee(Boolean isCancellationFee) { this.isCancellationFee = isCancellationFee; }
    public Boolean getIsDiscount() { return isDiscount; }
    public void setIsDiscount(Boolean isDiscount) { this.isDiscount = isDiscount; }
    public OffsetDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(OffsetDateTime voidedAt) { this.voidedAt = voidedAt; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public User getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
