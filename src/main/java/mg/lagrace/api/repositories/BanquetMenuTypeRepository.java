package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.BanquetMenuType;

public interface BanquetMenuTypeRepository extends JpaRepository<BanquetMenuType, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
