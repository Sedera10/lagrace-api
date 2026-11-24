package mg.lagrace.api.booking.pool;

import jakarta.persistence.*;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.pool.PoolRate;
import mg.lagrace.api.catalog.pool.PoolService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "booking_pool")
public class BookingPool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_booking_pool")
    private Long idBookingPool;

    @Column(name = "ref", nullable = false, unique = true, length = 20)
    private String ref;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_service_id", nullable = false)
    private PoolService poolService;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_rate_id", nullable = false)
    private PoolRate poolRate;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(name = "guests_count")
    private Integer guestsCount;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "included", nullable = false)
    private Boolean included = false;

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
        if (quantity == null) quantity = 1;
        if (included == null) included = false;
        if (active == null) active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getIdBookingPool() { return idBookingPool; }
    public void setIdBookingPool(Long idBookingPool) { this.idBookingPool = idBookingPool; }
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Pool getPool() { return pool; }
    public void setPool(Pool pool) { this.pool = pool; }
    public PoolService getPoolService() { return poolService; }
    public void setPoolService(PoolService poolService) { this.poolService = poolService; }
    public PoolRate getPoolRate() { return poolRate; }
    public void setPoolRate(PoolRate poolRate) { this.poolRate = poolRate; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public Integer getGuestsCount() { return guestsCount; }
    public void setGuestsCount(Integer guestsCount) { this.guestsCount = guestsCount; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Boolean getIncluded() { return included; }
    public void setIncluded(Boolean included) { this.included = included; }
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
