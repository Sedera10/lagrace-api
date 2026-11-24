package mg.lagrace.api.auth.application;

import java.util.Arrays;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import mg.lagrace.api.exceptions.BusinessException;

/** Reads live RBAC assignments for each request; JWT authorities are intentionally not cached here. */
@Service
public class BookingPermissionService {
    private final JdbcTemplate jdbc;

    public BookingPermissionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isAuthorized(long userId, String permissionCode) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users u " +
                "JOIN user_role ur ON ur.user_id=u.id_user AND ur.is_active " +
                "JOIN role_permission rp ON rp.role_id=ur.role_id AND rp.is_active " +
                "WHERE u.id_user=? AND u.is_active AND rp.permission_code=?", Integer.class,
                userId, permissionCode);
        return count != null && count > 0;
    }

    public boolean isAuthorized(Authentication authentication, String permissionCode) {
        if (authentication == null || authentication.getName() == null) return false;
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users u " +
                "JOIN user_role ur ON ur.user_id=u.id_user AND ur.is_active " +
                "JOIN role_permission rp ON rp.role_id=ur.role_id AND rp.is_active " +
                "WHERE u.username=? AND u.is_active AND rp.permission_code=?", Integer.class,
                authentication.getName(), permissionCode);
        return count != null && count > 0;
    }

    public boolean isAuthorized(String username, String permissionCode) {
        if (username == null || username.isBlank()) return false;
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users u " +
                "JOIN user_role ur ON ur.user_id=u.id_user AND ur.is_active " +
                "JOIN role_permission rp ON rp.role_id=ur.role_id AND rp.is_active " +
                "WHERE u.username=? AND u.is_active AND rp.permission_code=?", Integer.class,
                username, permissionCode);
        return count != null && count > 0;
    }

    public void require(Authentication authentication, String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (!isAuthorized(authentication, permissionCode)) {
                throw new BusinessException("FORBIDDEN",
                        "Vous n'avez pas la permission requise pour effectuer cette action.");
            }
        }
    }

    public void requireField(Authentication authentication, String field, String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (!isAuthorized(authentication, permissionCode)) {
                throw new BusinessException("FORBIDDEN",
                        "Vous n'avez pas la permission requise pour effectuer cette action.", field);
            }
        }
    }

    public void require(String username, String field, String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (!isAuthorized(username, permissionCode)) {
                throw new BusinessException("FORBIDDEN",
                        "Vous n'avez pas la permission requise pour effectuer cette action.", field);
            }
        }
    }

    public boolean hasAny(Authentication authentication, String... permissionCodes) {
        return Arrays.stream(permissionCodes).anyMatch(code -> isAuthorized(authentication, code));
    }
}
