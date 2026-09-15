package mg.lagrace.api.services;

import org.springframework.stereotype.Service;

import mg.lagrace.api.repositories.RevokedTokenRepository;
import mg.lagrace.api.repositories.UserRepository;
import mg.lagrace.api.models.RevokedToken;
import mg.lagrace.api.models.User;
import mg.lagrace.api.dto.UserResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class AuthService implements UserDetailsService {
    private final UserRepository userRepo;
    private final RevokedTokenRepository repository;

    public AuthService(UserRepository userRepo,RevokedTokenRepository repository) {
        this.userRepo = userRepo;
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));

        var authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!user.isActive())
                .build();
    }

    public void revoke(String tokenId, Instant expiresAt) {
        if (!repository.existsByTokenId(tokenId)) {
            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setTokenId(tokenId);
            revokedToken.setExpiresAt(expiresAt);
            repository.save(revokedToken);
        }
    }

    public boolean isRevoked(String tokenId) {
        return repository.existsByTokenId(tokenId);
    }

        @Transactional(readOnly = true)
        public UserResponse getUserResponse(String username) {
        User user = userRepo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));

        var roles = user.getRoles().stream()
            .map(role -> role.getName())
            .collect(Collectors.toSet());

        return new UserResponse(
            user.getIdUser(),
            user.getLastName(),
            user.getFirstName(),
            user.getBirthDate(),
            user.getUsername(),
            user.getEmail(),
            user.isActive(),
            roles);
        }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpiredTokens() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }
}