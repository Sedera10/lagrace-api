package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "unit")
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unit")
    private Long idUnit;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    public Long getIdUnit() { return idUnit; }
    public void setIdUnit(Long idUnit) { this.idUnit = idUnit; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

}
