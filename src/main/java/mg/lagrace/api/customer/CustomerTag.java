package mg.lagrace.api.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

@Entity
@Table(name = "customer_tag")
@Check(constraints = "color_code ~ '^#[0-9A-Fa-f]{6}$'")
public class CustomerTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tag")
    private Long idTag;

    @Column(name = "name", nullable = false, unique = true, length = 40)
    private String name;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public Long getIdTag() {
        return idTag;
    }

    public void setIdTag(Long idTag) {
        this.idTag = idTag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
