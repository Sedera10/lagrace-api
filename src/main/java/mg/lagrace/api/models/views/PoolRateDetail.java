package mg.lagrace.api.models.views;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_pool_rate")
public class PoolRateDetail {

    @Id
    @Column(name = "id_rate")
    private Long idRate;

    @Column(name = "pool_id")
    private Long poolId;

    @Column(name = "id_service")
    private Long idService;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "service_description")
    private String serviceDescription;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "is_temporary")
    private Boolean temporary;

    @Column(name = "meal_included")
    private Boolean mealIncluded;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "age_label")
    private String ageLabel;

    @Column(name = "meal_label")
    private String mealLabel;

    @Column(name = "id_unit")
    private Long idUnit;

    @Column(name = "unit_name")
    private String unitName;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Getters / Setters

    public Long getIdRate() { return idRate; }
    public Long getPoolId() { return poolId; }
    public Long getIdService() { return idService; }
    public String getServiceCode() { return serviceCode; }
    public String getServiceName() { return serviceName; }
    public String getServiceDescription() { return serviceDescription; }
    public BigDecimal getAmount() { return amount; }
    public Boolean getTemporary() { return temporary; }
    public Boolean getMealIncluded() { return mealIncluded; }
    public Integer getMinAge() { return minAge; }
    public Integer getMaxAge() { return maxAge; }
    public String getAgeLabel() { return ageLabel; }
    public String getMealLabel() { return mealLabel; }
    public Long getIdUnit() { return idUnit; }
    public String getUnitName() { return unitName; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}