package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_method")
public class PaymentMethod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_method")
    private Long idMethod;
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    public Long getIdMethod() { return idMethod; }
    public void setIdMethod(Long idMethod) { this.idMethod = idMethod; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
