package mg.lagrace.api.models;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "sex")
public class Sex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sex")
    private Long idSex;

    @Column(name = "name", length = 20, nullable = false, unique = true)
    private String name;

    @Column(name = "code", length = 1, nullable = false, unique = true)
    private String code;

    // Constructeurs
    public Sex() {}

    public Sex(String name, String code) {
        this.name = name;
        this.code = code;
    }

    // Getters / Setters
    public Long getIdSex() { return idSex; }
    public void setIdSex(Long idSex) { this.idSex = idSex; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    // equals / hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sex)) return false;
        Sex sex = (Sex) o;
        return Objects.equals(idSex, sex.idSex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSex);
    }

    @Override
    public String toString() {
        return "Sex{idSex=" + idSex + ", name='" + name + "', code='" + code + "'}";
    }
}