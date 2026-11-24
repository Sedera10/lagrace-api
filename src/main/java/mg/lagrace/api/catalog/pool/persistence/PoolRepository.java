package mg.lagrace.api.catalog.pool.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.pool.Pool;

public interface PoolRepository extends JpaRepository<Pool, Long> {
	java.util.List<Pool> findAllByOrderByIdPoolAsc();
}