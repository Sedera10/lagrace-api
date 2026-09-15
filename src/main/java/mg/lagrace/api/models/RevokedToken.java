package mg.lagrace.api.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revoked_token")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", nullable = false, unique = true, length = 100)
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt = Instant.now();

    // Getters et setters
    public Long getId() { return id; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getTokenId() { return tokenId; }

    public void setId(Long id) { this.id = id; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
}
