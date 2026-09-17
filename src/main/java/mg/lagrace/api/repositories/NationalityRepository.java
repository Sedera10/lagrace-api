package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.Nationality;

public interface NationalityRepository extends JpaRepository<Nationality,Long> {
    boolean existsByName(String name);
    boolean existsByCountryCode(String countryCode);
}
