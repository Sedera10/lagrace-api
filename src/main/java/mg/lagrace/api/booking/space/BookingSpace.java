package mg.lagrace.api.booking.space;

import jakarta.persistence.*;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.catalog.space.Space;
import mg.lagrace.api.catalog.space.SpaceEvent;
import mg.lagrace.api.reference.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "booking_space")
public class BookingSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_booking_space")
    private Long idBookingSpace;

    @Column(name = "ref", nullable = false, unique = true, length = 20)
    @Generated(event = EventType.INSERT)
    private String ref;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id")
    private SpaceEvent eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "guests_count", nullable = false)
    private Integer guestsCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "included", nullable = false)
    private Boolean included = false;

    @Column(name = "amount", insertable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "with_caterer", nullable = false)
    private Boolean withCaterer = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private BookingLineStatus status;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (quantity == null) quantity = 1;
        if (included == null) included = false;
        if (withCaterer == null) withCaterer = false;
        if (active == null) active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getIdBookingSpace() { return idBookingSpace; }
    public void setIdBookingSpace(Long idBookingSpace) { this.idBookingSpace = idBookingSpace; }
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public SpaceEvent getEventType() { return eventType; }
    public void setEventType(SpaceEvent eventType) { this.eventType = eventType; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public Integer getGuestsCount() { return guestsCount; }
    public void setGuestsCount(Integer guestsCount) { this.guestsCount = guestsCount; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Boolean getIncluded() { return included; }
    public void setIncluded(Boolean included) { this.included = included; }
    public BigDecimal getAmount() { return amount; }
    public Boolean getWithCaterer() { return withCaterer; }
    public void setWithCaterer(Boolean withCaterer) { this.withCaterer = withCaterer; }
    public BookingLineStatus getStatus() { return status; }
    public void setStatus(BookingLineStatus status) { this.status = status; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
