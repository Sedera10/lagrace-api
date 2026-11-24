package mg.lagrace.api.catalog.space.persistence;

import mg.lagrace.api.catalog.space.Space;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.space.BanquetMenuType;

public interface BanquetMenuTypeRepository extends JpaRepository<BanquetMenuType, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
