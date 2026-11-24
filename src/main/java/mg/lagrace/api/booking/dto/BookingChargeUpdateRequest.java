package mg.lagrace.api.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

public class BookingChargeUpdateRequest {
    @NotNull private Integer version;
    private Integer quantity;
    private Integer persons;
    private Boolean included;
    private String reason;
    @JsonIgnore private final Set<String> present = new HashSet<>();
    @JsonProperty("version") public void setVersion(Integer value) { version = value; present.add("version"); }
    @JsonProperty("quantity") public void setQuantity(Integer value) { quantity = value; present.add("quantity"); }
    @JsonProperty("persons") public void setPersons(Integer value) { persons = value; present.add("persons"); }
    @JsonProperty("included") public void setIncluded(Boolean value) { included = value; present.add("included"); }
    @JsonProperty("reason") public void setReason(String value) { reason = value; present.add("reason"); }
    public Integer getVersion() { return version; }
    public Integer getQuantity() { return quantity; }
    public Integer getPersons() { return persons; }
    public Boolean getIncluded() { return included; }
    public String getReason() { return reason; }
    public boolean has(String field) { return present.contains(field); }
}
