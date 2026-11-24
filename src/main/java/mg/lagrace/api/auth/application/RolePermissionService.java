package mg.lagrace.api.auth.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;

@Service
public class RolePermissionService {
    private final JdbcTemplate jdbc;

    public RolePermissionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record RoleSummary(long id, String name, String description) {}
    public record Permission(String code, String label, boolean granted) {}
    public record Module(String code, String label, List<Permission> permissions) {}
    public record RolePermissions(long roleId, String roleName, int version, List<Module> modules) {}
    public record SavePermissions(Integer version, Set<String> permissionCodes, String reason) {}

    @Transactional(readOnly = true)
    public List<RoleSummary> roles(String actor) {
        requireAccess(actor);
        return jdbc.query("SELECT id_role, name, description FROM role ORDER BY name",
                (rs, n) -> new RoleSummary(rs.getLong(1), rs.getString(2), rs.getString(3)));
    }

    @Transactional(readOnly = true)
    public List<Module> catalog(String actor) {
        requireAccess(actor);
        return jdbc.query("SELECT m.code, m.label, p.code, p.label FROM permission_module m " +
                        "LEFT JOIN permission p ON p.module_code=m.code ORDER BY m.display_order, p.display_order",
                rs -> {
                    java.util.LinkedHashMap<String, java.util.ArrayList<Permission>> grouped = new java.util.LinkedHashMap<>();
                    java.util.LinkedHashMap<String, String> labels = new java.util.LinkedHashMap<>();
                    while (rs.next()) {
                        String module = rs.getString(1);
                        labels.putIfAbsent(module, rs.getString(2));
                        if (rs.getString(3) != null) grouped.computeIfAbsent(module, key -> new java.util.ArrayList<>())
                                .add(new Permission(rs.getString(3), rs.getString(4), false));
                    }
                    return labels.keySet().stream().map(code -> new Module(code, labels.get(code), grouped.getOrDefault(code, new java.util.ArrayList<>()))).toList();
                });
    }

    @Transactional(readOnly = true)
    public RolePermissions getRole(long roleId, String actor) {
        requireAccess(actor);
        return rolePermissions(roleId);
    }

    private RolePermissions rolePermissions(long roleId) {
        RoleSummary role = role(roleId);
        int version = jdbc.queryForObject("SELECT version FROM role WHERE id_role=?", Integer.class, roleId);
        List<Module> modules = jdbc.query("SELECT m.code,m.label,p.code,p.label,COALESCE(rp.is_active,FALSE) " +
                        "FROM permission_module m LEFT JOIN permission p ON p.module_code=m.code " +
                        "LEFT JOIN role_permission rp ON rp.permission_code=p.code AND rp.role_id=? " +
                        "ORDER BY m.display_order,p.display_order",
                rs -> {
                    java.util.LinkedHashMap<String, java.util.ArrayList<Permission>> grouped = new java.util.LinkedHashMap<>();
                    java.util.LinkedHashMap<String, String> labels = new java.util.LinkedHashMap<>();
                    while (rs.next()) {
                        String module = rs.getString(1); labels.putIfAbsent(module, rs.getString(2));
                        if (rs.getString(3) != null) grouped.computeIfAbsent(module, key -> new java.util.ArrayList<>())
                                .add(new Permission(rs.getString(3), rs.getString(4), rs.getBoolean(5)));
                    }
                    return labels.keySet().stream().map(code -> new Module(code, labels.get(code), grouped.getOrDefault(code, new java.util.ArrayList<>()))).toList();
                }, roleId);
        return new RolePermissions(roleId, role.name(), version, modules);
    }

    @Transactional
    public RolePermissions save(long roleId, String actor, SavePermissions request) {
        // Serialize both authorization and mutation; a concurrent request cannot
        // revoke this actor's access after it was checked but before this write.
        jdbc.execute("SELECT pg_advisory_xact_lock(78123041)");
        requireAccess(actor);
        if (request == null || request.version() == null || request.permissionCodes() == null)
            throw new ValidationException("La version et la sélection des permissions sont obligatoires.");
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty() || reason.length() > 255)
            throw new ValidationException("Le motif est obligatoire et limité à 255 caractères.", "reason");

        // Serialize edits across roles so concurrent requests cannot both remove the last administrator.
        RoleSummary role = role(roleId);
        Integer currentVersion = jdbc.queryForObject("SELECT version FROM role WHERE id_role=? FOR UPDATE", Integer.class, roleId);
        if (!currentVersion.equals(request.version()))
            throw new ConflictException("STALE_VERSION", "Les permissions ont changé depuis leur chargement. Rechargez le rôle.");

        Set<String> known = Set.copyOf(jdbc.query("SELECT code FROM permission", (rs, n) -> rs.getString(1)));
        if (!known.containsAll(request.permissionCodes()))
            throw new ValidationException("La sélection contient une permission inconnue.", "permissionCodes");

        List<String> active = jdbc.query("SELECT permission_code FROM role_permission WHERE role_id=? AND is_active ORDER BY permission_code",
                (rs, n) -> rs.getString(1), roleId);
        Set<String> before = Set.copyOf(active);
        Set<String> selected = request.permissionCodes();
        if (before.equals(selected)) return rolePermissions(roleId);

        long administrators = jdbc.queryForObject("SELECT COUNT(DISTINCT u.id_user) FROM users u " +
                "JOIN user_role ur ON ur.user_id=u.id_user AND ur.is_active " +
                "JOIN role_permission rp ON rp.role_id=ur.role_id AND rp.is_active " +
                "WHERE u.is_active AND rp.permission_code='ACCESS_MANAGE'", Long.class);
        boolean losesAdmin = before.contains("ACCESS_MANAGE") && !selected.contains("ACCESS_MANAGE");
        if (losesAdmin) {
            long alternate = jdbc.queryForObject("SELECT COUNT(DISTINCT u.id_user) FROM users u " +
                    "JOIN user_role ur ON ur.user_id=u.id_user AND ur.is_active " +
                    "JOIN role_permission rp ON rp.role_id=ur.role_id AND rp.is_active " +
                    "WHERE u.is_active AND rp.permission_code='ACCESS_MANAGE' AND ur.role_id<>?", Long.class, roleId);
            if (administrators <= 1 || alternate == 0)
                throw new ConflictException("LAST_ADMIN_ACCESS", "Impossible de retirer le dernier accès administrateur actif.");
        }

        UUID operationId = UUID.randomUUID();
        Long actorId = jdbc.queryForObject("SELECT id_user FROM users WHERE username=?", Long.class, actor);
        for (String code : known) {
            boolean wasGranted = before.contains(code), isGranted = selected.contains(code);
            if (wasGranted == isGranted) continue;
            jdbc.update("INSERT INTO role_permission(role_id,permission_code,is_active,updated_at) VALUES(?,?,?,CURRENT_TIMESTAMP) " +
                    "ON CONFLICT(role_id,permission_code) DO UPDATE SET is_active=EXCLUDED.is_active, updated_at=CURRENT_TIMESTAMP",
                    roleId, code, isGranted);
            jdbc.update("INSERT INTO access_change_log(operation_id,actor_user_id,event_type,role_id,permission_code,old_value,new_value,reason) " +
                    "VALUES(?,?,?,?,?,?,?,?)", operationId, actorId, isGranted ? "PERMISSION_GRANTED" : "PERMISSION_REVOKED",
                    roleId, code, Boolean.toString(wasGranted), Boolean.toString(isGranted), reason);
        }
        jdbc.update("UPDATE role SET version=version+1 WHERE id_role=?", roleId);
        return rolePermissions(roleId);
    }

    private RoleSummary role(long id) {
        RoleSummary result = jdbc.query("SELECT id_role,name,description FROM role WHERE id_role=?",
                rs -> rs.next() ? new RoleSummary(rs.getLong(1), rs.getString(2), rs.getString(3)) : null, id);
        if (result == null) throw new ResourceNotFoundException("Rôle introuvable.");
        return result;
    }

    private void requireAccess(String username) {
        if (username == null || username.isBlank()) throw new AccessDeniedException("ACCESS_MANAGE requis.");
        Integer allowed = jdbc.queryForObject("SELECT COUNT(*) FROM users u JOIN user_role ur ON ur.user_id=u.id_user AND ur.is_active " +
                "JOIN role_permission rp ON rp.role_id=ur.role_id AND rp.is_active " +
                "WHERE u.username=? AND u.is_active AND rp.permission_code='ACCESS_MANAGE'", Integer.class, username);
        if (allowed == null || allowed == 0) throw new AccessDeniedException("ACCESS_MANAGE requis.");
    }
}
