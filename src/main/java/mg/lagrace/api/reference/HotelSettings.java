package mg.lagrace.api.reference;

import java.time.OffsetDateTime;
import jakarta.persistence.*;

@Entity @Table(name="hotel_settings")
public class HotelSettings {
    @Id private Long id; 
    
    @Column(name="legal_name", nullable=false, length=200)
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
    
    @Column(name="updated_at", nullable=false)
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
    public void setLegalName(String legalName){this.legalName=legalName;}
    public void setAddress(String address){this.address=address;}
    public void setPhone(String phone){this.phone=phone;}
    public void setEmail(String email){this.email=email;}
    public void setNif(String nif){this.nif=nif;}
    public void setStat(String stat){this.stat=stat;}
    public void setPaymentDetails(String paymentDetails){this.paymentDetails=paymentDetails;}
    public void setInvoiceFooter(String invoiceFooter){this.invoiceFooter=invoiceFooter;}
    public void setUpdatedAt(OffsetDateTime updatedAt){this.updatedAt=updatedAt;}
}
