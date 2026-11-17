package mg.lagrace.api.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_customer")
    private Long idCustomer;

    @Column(name = "lastname", nullable = false, length = 100)
    private String lastName;

    @Column(name = "firstname", length = 100)
    private String firstName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "identification_number", length = 100)
    private String identificationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private IdentificationDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nationality_id")
    private Nationality nationality;

    @Column(name = "phone_key", insertable = false, updatable = false, length = 9)
    private String phoneKey;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_id")
    private Customer mergedInto;

    @Column(name = "preferred_language", length = 5)
    private String preferredLanguage;

    @Column(name = "contact_consent", nullable = false)
    private boolean contactConsent;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getIdCustomer() { return idCustomer; }
    public void setIdCustomer(Long idCustomer) { this.idCustomer = idCustomer; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getIdentificationNumber() { return identificationNumber; }
    public void setIdentificationNumber(String identificationNumber) { this.identificationNumber = identificationNumber; }
    public IdentificationDocument getDocument() { return document; }
    public void setDocument(IdentificationDocument document) { this.document = document; }
    public Nationality getNationality() { return nationality; }
    public void setNationality(Nationality nationality) { this.nationality = nationality; }
    public String getPhoneKey() { return phoneKey; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Customer getMergedInto() { return mergedInto; }
    public void setMergedInto(Customer mergedInto) { this.mergedInto = mergedInto; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public boolean isContactConsent() { return contactConsent; }
    public void setContactConsent(boolean contactConsent) { this.contactConsent = contactConsent; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
