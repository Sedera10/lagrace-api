package mg.lagrace.api.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "pool_rate")
public class PoolRate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rate", columnDefinition = "integer")
    private Long idRate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, columnDefinition = "integer")
    private PoolService service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false, columnDefinition = "integer")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false, columnDefinition = "integer")
    private Pool pool;

    @Column(name = "meal_included")
    private Boolean isMealIncluded;

    @Column(name = "min_age", nullable = true)
    private Integer minAge;

    @Column(name = "max_age", nullable = true)
    private Integer maxAge;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = true)
    private LocalDate validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
    }


    public Long getIdRate() { return idRate; }
    public void setIdRate(Long idRate) { this.idRate = idRate; }
    public PoolService getService() { return service; }
    public void setService(PoolService service) { this.service = service; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public Pool getPool() { return pool; }
    public void setPool(Pool pool) { this.pool = pool; }
    public Boolean getIsMealIncluded() { return isMealIncluded; }
    public void setIsMealIncluded(Boolean isMealIncluded) { this.isMealIncluded = isMealIncluded; }
    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }
    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
    @Transient
    public String getAgeLabel() {
        if (maxAge != null && (minAge == null || minAge == 0 || minAge == 1)) {
            return "Moins de " + (maxAge) + " ans";
        }
        if (minAge != null && maxAge != null) {
            return minAge + " - " + maxAge + " ans";
        }
        if (minAge != null) {
            return (minAge - 1) + " ans et plus";
        }
        return null;
    }

    @Transient
    public String getMealLabel() {
        if (Boolean.TRUE.equals(isMealIncluded)) {
            return "Avec repas";
        }
        if (Boolean.FALSE.equals(isMealIncluded)) {
            return "Sans repas";
        }
        return null;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}