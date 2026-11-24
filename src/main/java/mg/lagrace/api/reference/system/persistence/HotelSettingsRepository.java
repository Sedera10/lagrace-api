package mg.lagrace.api.reference.system.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.reference.HotelSettings;
public interface HotelSettingsRepository extends JpaRepository<HotelSettings, Long> {}
