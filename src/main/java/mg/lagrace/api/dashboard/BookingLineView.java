package mg.lagrace.api.dashboard;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "v_booking_line")
public class BookingLineView {
    @EmbeddedId
    private BookingLineViewId id;

    @Column(name = "ref")
    private String reference;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "included")
    private Boolean included;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "status_id")
    private Long statusId;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "version")
    private Integer version;

    protected BookingLineView() {
    }

    public BookingLineViewId getId() { return id; }
    public String getReference() { return reference; }
    public Long getBookingId() { return bookingId; }
    public Long getResourceId() { return resourceId; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Boolean getIncluded() { return included; }
    public BigDecimal getAmount() { return amount; }
    public Long getStatusId() { return statusId; }
    public String getStatusCode() { return statusCode; }
    public Boolean getActive() { return active; }
    public Integer getVersion() { return version; }

    @Embeddable
    public static class BookingLineViewId implements Serializable {
        @Column(name = "line_type")
        private String lineType;

        @Column(name = "line_id")
        private Long lineId;

        protected BookingLineViewId() {
        }

        public String getLineType() { return lineType; }
        public Long getLineId() { return lineId; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof BookingLineViewId that)) return false;
            return Objects.equals(lineType, that.lineType) && Objects.equals(lineId, that.lineId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lineType, lineId);
        }
    }
}
