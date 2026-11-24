package mg.lagrace.api.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.customer.Customer;

@Entity
@Table(name = "invoice")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_invoice") private Long idInvoice;
    @Generated(event = EventType.INSERT)
    @Column(name = "invoice_number", insertable = false, updatable = false) private String invoiceNumber;
    @Column(name = "invoice_type", nullable = false) private String invoiceType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "booking_id") private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "status_id") private InvoiceStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "credited_invoice_id") private Invoice creditedInvoice;
    @Column(name = "credit_reason") private String creditReason;
    @Column(name = "issue_date") private LocalDate issueDate;
    @Column(name = "billing_name") private String billingName;
    @Column(name = "billing_address") private String billingAddress;
    @Column(name = "billing_phone") private String billingPhone;
    @Column(name = "billing_nationality") private String billingNationality;
    @Column(name = "billing_tax_id") private String billingTaxId;
    @Column(name = "reference_label") private String referenceLabel;
    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end") private LocalDate periodEnd;
    @Column(name = "subtotal") private BigDecimal subtotal;
    @Column(name = "discount") private BigDecimal discount;
    @Column(name = "total") private BigDecimal total;
    @Column(name = "notes") private String notes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id") private User createdByUser;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issuer_snapshot", columnDefinition = "jsonb") private String issuerSnapshot;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;


    public Long getIdInvoice(){return idInvoice;} public String getInvoiceNumber(){return invoiceNumber;} public String getInvoiceType(){return invoiceType;}
    public Customer getCustomer(){return customer;} public Booking getBooking(){return booking;} public InvoiceStatus getStatus(){return status;}
    public Invoice getCreditedInvoice(){return creditedInvoice;} public String getCreditReason(){return creditReason;} public LocalDate getIssueDate(){return issueDate;}
    public String getBillingName(){return billingName;} public String getBillingAddress(){return billingAddress;} public String getBillingPhone(){return billingPhone;}
    public String getBillingNationality(){return billingNationality;} public String getBillingTaxId(){return billingTaxId;} public String getReferenceLabel(){return referenceLabel;}
    public LocalDate getPeriodStart(){return periodStart;} public LocalDate getPeriodEnd(){return periodEnd;} public BigDecimal getSubtotal(){return subtotal;}
    public BigDecimal getDiscount(){return discount;} public BigDecimal getTotal(){return total;} public String getNotes(){return notes;}
    public String getIssuerSnapshot(){return issuerSnapshot;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
        public User getCreatedByUser(){return createdByUser;}
        public void setCreatedByUser(User value){createdByUser=value;} 
    public void setInvoiceType(String value){invoiceType=value;} public void setCustomer(Customer value){customer=value;} public void setBooking(Booking value){booking=value;} public void setStatus(InvoiceStatus value){status=value;} public void setCreditedInvoice(Invoice value){creditedInvoice=value;} public void setCreditReason(String value){creditReason=value;} public void setIssueDate(LocalDate value){issueDate=value;} public void setBillingName(String value){billingName=value;} public void setBillingAddress(String value){billingAddress=value;} public void setBillingPhone(String value){billingPhone=value;} public void setBillingNationality(String value){billingNationality=value;} public void setBillingTaxId(String value){billingTaxId=value;} public void setReferenceLabel(String value){referenceLabel=value;} public void setPeriodStart(LocalDate value){periodStart=value;} public void setPeriodEnd(LocalDate value){periodEnd=value;} public void setSubtotal(BigDecimal value){subtotal=value;} public void setDiscount(BigDecimal value){discount=value;} public void setTotal(BigDecimal value){total=value;} public void setNotes(String value){notes=value;} public void setIssuerSnapshot(String value){issuerSnapshot=value;} public void setCreatedAt(OffsetDateTime value){createdAt=value;} public void setUpdatedAt(OffsetDateTime value){updatedAt=value;}
}
