package mg.lagrace.api.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class BookingOtherUpdateRequest {
    @NotNull private Integer version;
    private String label;
    private BigDecimal amount;
    @JsonIgnore private final Set<String> present = new HashSet<>();
    @JsonProperty("version") public void setVersion(Integer value) { version = value; present.add("version"); }
    @JsonProperty("label") public void setLabel(String value) { label = value; present.add("label"); }
    @JsonProperty("amount") public void setAmount(BigDecimal value) { amount = value; present.add("amount"); }
    public Integer getVersion() { return version; }
    public String getLabel() { return label; }
    public BigDecimal getAmount() { return amount; }
    public boolean has(String field) { return present.contains(field); }
}
