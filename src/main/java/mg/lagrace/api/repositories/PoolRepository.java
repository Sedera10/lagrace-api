package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.Pool;

public interface PoolRepository extends JpaRepository<Pool, Long> {
}