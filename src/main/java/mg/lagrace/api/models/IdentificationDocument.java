package mg.lagrace.api.models;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "identification_document")
public class IdentificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_document")
    private Long idDocument;

    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    // Constructeurs
    public IdentificationDocument() {}

    public IdentificationDocument(String name) {
        this.name = name;
    }

    // Getters / Setters
    public Long getIdDocument() { return idDocument; }
    public void setIdDocument(Long idDocument) { this.idDocument = idDocument; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // equals / hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentificationDocument)) return false;
        IdentificationDocument that = (IdentificationDocument) o;
        return Objects.equals(idDocument, that.idDocument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDocument);
    }

    @Override
    public String toString() {
        return "IdentificationDocument{idDocument=" + idDocument + ", name='" + name + "'}";
    }
}