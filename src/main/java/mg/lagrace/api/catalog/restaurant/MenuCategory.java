package mg.lagrace.api.catalog.restaurant;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_category")
public class MenuCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_menu_category")
    private Long idMenuCategory;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "position", nullable = false)
    private Integer position = 0;

    public Long getIdMenuCategory() { return idMenuCategory; }
    public void setIdMenuCategory(Long idMenuCategory) { this.idMenuCategory = idMenuCategory; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
