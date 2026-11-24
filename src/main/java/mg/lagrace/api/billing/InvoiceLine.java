package mg.lagrace.api.billing;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "invoice_line")
public class InvoiceLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name="id_invoice_line") private Long idInvoiceLine;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_id") private Invoice invoice;
    @Column(name="description") private String description;
    @Column(name="quantity") private BigDecimal quantity;
    @Column(name="unit_price") private BigDecimal unitPrice;
    @Column(name="amount", insertable=false, updatable=false) private BigDecimal amount;
    @Column(name="position") private Integer position;
    @Column(name="service_code") private String serviceCode;
    @Column(name="source_type") private String sourceType;
    @Column(name="source_id") private Long sourceId;
    @Column(name="is_billed") private Boolean billed;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="credited_line_id") private InvoiceLine creditedLine;
    public Long getIdInvoiceLine(){return idInvoiceLine;} public Invoice getInvoice(){return invoice;} public String getDescription(){return description;}
    public BigDecimal getQuantity(){return quantity;} public BigDecimal getUnitPrice(){return unitPrice;} public BigDecimal getAmount(){return amount;}
    public Integer getPosition(){return position;} public String getServiceCode(){return serviceCode;} public String getSourceType(){return sourceType;}
    public Long getSourceId(){return sourceId;} public Boolean getBilled(){return billed;} public InvoiceLine getCreditedLine(){return creditedLine;}
    public void setInvoice(Invoice value){invoice=value;} public void setDescription(String value){description=value;} public void setQuantity(BigDecimal value){quantity=value;} public void setUnitPrice(BigDecimal value){unitPrice=value;} public void setPosition(Integer value){position=value;} public void setServiceCode(String value){serviceCode=value;} public void setSourceType(String value){sourceType=value;} public void setSourceId(Long value){sourceId=value;} public void setBilled(Boolean value){billed=value;} public void setCreditedLine(InvoiceLine value){creditedLine=value;}
}
