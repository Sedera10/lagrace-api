package mg.lagrace.api.models;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "nationality")
public class Nationality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nationality")
    private Long idNationality;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "country_code", length = 2, nullable = false, unique = true, columnDefinition = "CHAR(2)")
    private String countryCode;

    @Column(name = "flag_emoji", length = 10)
    private String flagEmoji;

    // Constructeurs
    public Nationality() {}

    public Nationality(String name, String countryCode, String flagEmoji) {
        this.name = name;
        this.countryCode = countryCode;
        this.flagEmoji = flagEmoji;
    }

    // Getters / Setters
    public Long getIdNationality() { return idNationality; }
    public void setIdNationality(Long idNationality) { this.idNationality = idNationality; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getFlagEmoji() { return flagEmoji; }
    public void setFlagEmoji(String flagEmoji) { this.flagEmoji = flagEmoji; }

    // equals / hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nationality)) return false;
        Nationality that = (Nationality) o;
        return Objects.equals(idNationality, that.idNationality);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idNationality);
    }

    @Override
    public String toString() {
        return "Nationality{idNationality=" + idNationality +
               ", name='" + name + "', countryCode='" + countryCode +
               "', flagEmoji='" + flagEmoji + "'}";
    }
}
