package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "pool_service")
public class PoolService {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_service")
    private Long idService;
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public Long getIdService() { return idService; }
    public void setIdService(Long idService) { this.idService = idService; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
