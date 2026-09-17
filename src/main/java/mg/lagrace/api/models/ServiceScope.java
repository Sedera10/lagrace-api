package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "service_scope")
public class ServiceScope {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_scope")
    private Long idScope;
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    public Long getIdScope() { return idScope; }
    public void setIdScope(Long idScope) { this.idScope = idScope; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
