package mg.lagrace.api.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public final class CustomerMutationRequests {
    private CustomerMutationRequests() {}

        public static final class Update {
                @NotNull private Integer version;
                @Size(max = 100) private String lastname;
                @Size(max = 100) private String firstname;
                private LocalDate dateOfBirth;
                @Size(max = 30) private String phone;
                @Size(max = 255) private String email;
                @Size(max = 255) private String address;
                private Long nationalityId;
                private Long documentId;
                @Size(max = 100) private String identificationNumber;
                @Size(max = 5) private String preferredLanguage;
                private Boolean contactConsent;
                @Size(max = 255) private String reason;
                @JsonIgnore private final Set<String> present = new HashSet<>();

                @JsonProperty("version") public void setVersion(Integer value) { version = value; }
                @JsonProperty("lastname") public void setLastname(String value) { present.add("lastname"); lastname = value; }
                @JsonProperty("firstname") public void setFirstname(String value) { present.add("firstname"); firstname = value; }
                @JsonProperty("dateOfBirth") public void setDateOfBirth(LocalDate value) { present.add("dateOfBirth"); dateOfBirth = value; }
                @JsonProperty("phone") public void setPhone(String value) { present.add("phone"); phone = value; }
                @JsonProperty("email") public void setEmail(String value) { present.add("email"); email = value; }
                @JsonProperty("address") public void setAddress(String value) { present.add("address"); address = value; }
                @JsonProperty("nationalityId") public void setNationalityId(Long value) { present.add("nationalityId"); nationalityId = value; }
                @JsonProperty("documentId") public void setDocumentId(Long value) { present.add("documentId"); documentId = value; }
                @JsonProperty("identificationNumber") public void setIdentificationNumber(String value) { present.add("identificationNumber"); identificationNumber = value; }
                @JsonProperty("preferredLanguage") public void setPreferredLanguage(String value) { present.add("preferredLanguage"); preferredLanguage = value; }
                @JsonProperty("contactConsent") public void setContactConsent(Boolean value) { present.add("contactConsent"); contactConsent = value; }
                @JsonProperty("reason") public void setReason(String value) { reason = value; }

                public Integer version() { return version; }
                public String lastname() { return lastname; }
                public String firstname() { return firstname; }
                public LocalDate dateOfBirth() { return dateOfBirth; }
                public String phone() { return phone; }
                public String email() { return email; }
                public String address() { return address; }
                public Long nationalityId() { return nationalityId; }
                public Long documentId() { return documentId; }
                public String identificationNumber() { return identificationNumber; }
                public String preferredLanguage() { return preferredLanguage; }
                public Boolean contactConsent() { return contactConsent; }
                public String reason() { return reason; }
                public boolean has(String field) { return present.contains(field); }
                public boolean hasChanges() { return !present.isEmpty(); }
        }

        public record Version(@NotNull Integer version, @Size(max = 255) String reason) {}
        public record Reason(@Size(max = 255) String reason) {}

    public record NoteCreate(@NotBlank String noteType,
            @NotBlank String content, Boolean isPinned, Boolean isAlert) {}

    public record NoteUpdate(@NotNull Integer version, @NotBlank String content,
            @NotBlank String noteType, Boolean isPinned, Boolean isAlert) {}

    public record NoteVersion(@NotNull Integer version) {}

    public record TagReplace(@NotNull List<Long> tagIds, @NotNull Integer version) {}

    public record CustomerTag(@NotBlank @Size(max = 40) String name,
            @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorCode,
            @NotNull Boolean isActive) {}

    public enum MergeFieldChoice { KEPT, MERGED }
    public record MergeFieldChoices(MergeFieldChoice phone, MergeFieldChoice email,
            MergeFieldChoice address, MergeFieldChoice dateOfBirth,
            MergeFieldChoice nationality, MergeFieldChoice documentType,
            MergeFieldChoice preferredLanguage) {}
    public record Merge(@NotNull Long mergedId, @NotNull Integer keptVersion,
            @NotNull Integer mergedVersion, MergeFieldChoices fieldChoices,
            @NotBlank @Size(max = 255) String reason) {}
}