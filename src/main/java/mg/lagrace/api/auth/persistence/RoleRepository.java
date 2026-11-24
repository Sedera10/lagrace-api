package mg.lagrace.api.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.auth.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByName(String name);
}
