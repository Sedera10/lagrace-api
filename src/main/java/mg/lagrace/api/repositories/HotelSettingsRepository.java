package mg.lagrace.api.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.HotelSettings;
public interface HotelSettingsRepository extends JpaRepository<HotelSettings, Long> {}
