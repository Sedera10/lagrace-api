package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "department")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_department")
    private Long idDepartment;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    public Long getIdDepartment() { return idDepartment; }
    public void setIdDepartment(Long idDepartment) { this.idDepartment = idDepartment; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
