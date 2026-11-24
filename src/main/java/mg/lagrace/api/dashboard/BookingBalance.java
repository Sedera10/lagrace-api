package mg.lagrace.api.dashboard;

import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "v_booking_balance")
public class BookingBalance {
    @Id
    @Column(name = "id_booking")
    private Long idBooking;

    @Column(name = "lines_total")
    private BigDecimal linesTotal;

    @Column(name = "charges_total")
    private BigDecimal chargesTotal;

    @Column(name = "others_total")
    private BigDecimal othersTotal;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "paid")
    private BigDecimal paid;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "deposit_due")
    private BigDecimal depositDue;

    @Column(name = "deposit_held")
    private BigDecimal depositHeld;

    protected BookingBalance() {
    }

    public Long getIdBooking() { return idBooking; }
    public BigDecimal getLinesTotal() { return linesTotal; }
    public BigDecimal getChargesTotal() { return chargesTotal; }
    public BigDecimal getOthersTotal() { return othersTotal; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getPaid() { return paid; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getDepositDue() { return depositDue; }
    public BigDecimal getDepositHeld() { return depositHeld; }
}
