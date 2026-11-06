package mg.lagrace.api.dto.booking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.NewCustomer;
import mg.lagrace.api.models.enums.BookingOrigin;

public class BookingUpdateRequest {
    @NotNull
    private Integer version;
    private Long customerId;
    @Valid private NewCustomer newCustomer;
    private BookingOrigin origin;
    @Size(max = 2000) private String notes;
    @Valid private List<OtherChange> others;
    @JsonIgnore private final Set<String> present = new HashSet<>();

    @JsonProperty("version") public void setVersion(Integer value) { present.add("version"); version = value; }
    @JsonProperty("customerId") public void setCustomerId(Long value) { present.add("customerId"); customerId = value; }
    @JsonProperty("newCustomer") public void setNewCustomer(NewCustomer value) { present.add("newCustomer"); newCustomer = value; }
    @JsonProperty("origin") public void setOrigin(BookingOrigin value) { present.add("origin"); origin = value; }
    @JsonProperty("notes") public void setNotes(String value) { present.add("notes"); notes = value; }
    @JsonProperty("others") public void setOthers(List<OtherChange> value) { present.add("others"); others = value; }
    public Integer getVersion() { return version; }
    public Long getCustomerId() { return customerId; }
    public NewCustomer getNewCustomer() { return newCustomer; }
    public BookingOrigin getOrigin() { return origin; }
    public String getNotes() { return notes; }
    public List<OtherChange> getOthers() { return others; }
    public boolean has(String name) { return present.contains(name); }

    public record OtherChange(Long id, Integer version, String label, BigDecimal amount,
            Boolean voided, String voidReason) {}
}
