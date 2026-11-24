package mg.lagrace.api.booking.room;

import jakarta.persistence.*;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.catalog.room.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "booking_room")
public class BookingRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_booking_room")
    private Long idBookingRoom;

    @Column(name = "ref", nullable = false, unique = true, length = 20)
    @Generated(event = EventType.INSERT)
    private String ref;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "included", nullable = false)
    private Boolean included = false;

    @Column(name = "nights", insertable = false, updatable = false)
    private Integer nights;

    @Column(name = "amount", insertable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

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
        if (included == null) included = false;
        if (active == null) active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getIdBookingRoom() { return idBookingRoom; }
    public void setIdBookingRoom(Long idBookingRoom) { this.idBookingRoom = idBookingRoom; }
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Boolean getIncluded() { return included; }
    public void setIncluded(Boolean included) { this.included = included; }
    public Integer getNights() { return nights; }
    public BigDecimal getAmount() { return amount; }
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
