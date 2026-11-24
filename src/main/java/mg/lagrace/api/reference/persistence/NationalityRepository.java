package mg.lagrace.api.reference.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.reference.Nationality;

public interface NationalityRepository extends JpaRepository<Nationality,Long> {
    boolean existsByName(String name);
    boolean existsByCountryCode(String countryCode);
}
