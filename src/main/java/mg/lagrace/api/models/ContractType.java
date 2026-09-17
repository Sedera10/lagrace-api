package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "contract_type")
public class ContractType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contract_type")
    private Long idContractType;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    public Long getIdContractType() { return idContractType; }
    public void setIdContractType(Long idContractType) { this.idContractType = idContractType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
