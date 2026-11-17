package mg.lagrace.api.models;

import java.time.OffsetDateTime;
import org.hibernate.annotations.Immutable;
import jakarta.persistence.*;

@Entity @Immutable @Table(name="hotel_settings")
public class HotelSettings {
    @Id private Long id; 
    
    @Column(name="legal_name") 
    private String legalName; 
    
    private String address; 
    
    private String phone; 
    
    private String email; 
    
    private String nif; 
    
    private String stat;

    @Column(name="payment_details") 
    private String paymentDetails; 
    
    @Column(name="invoice_footer") 
    private String invoiceFooter; 
    
    @Column(name="updated_at") 
    private OffsetDateTime updatedAt;

    public Long getId(){return id;} 
    public String getLegalName(){return legalName;} 
    public String getAddress(){return address;} 
    public String getPhone(){return phone;} 
    public String getEmail(){return email;} 
    public String getNif(){return nif;} 
    public String getStat(){return stat;} 
    public String getPaymentDetails(){return paymentDetails;} 
    public String getInvoiceFooter(){return invoiceFooter;} 
    public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
