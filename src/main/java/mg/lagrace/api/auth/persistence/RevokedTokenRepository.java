package mg.lagrace.api.auth.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.auth.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenId(String tokenId);
    void deleteByExpiresAtBefore(Instant date);
}
