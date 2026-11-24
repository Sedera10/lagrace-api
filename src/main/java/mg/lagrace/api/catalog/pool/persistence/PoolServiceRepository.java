package mg.lagrace.api.catalog.pool.persistence;

import mg.lagrace.api.catalog.pool.Pool;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.pool.PoolService;

public interface PoolServiceRepository extends JpaRepository<PoolService, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
