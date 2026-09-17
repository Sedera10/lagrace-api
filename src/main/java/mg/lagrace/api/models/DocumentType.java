package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "document_type")
public class DocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type")
    private Long idType;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    public Long getIdType() { return idType; }
    public void setIdType(Long idType) { this.idType = idType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
