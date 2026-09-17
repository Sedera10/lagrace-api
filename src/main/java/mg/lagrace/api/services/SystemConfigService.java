package mg.lagrace.api.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import mg.lagrace.api.dto.system.CalendarDayRequest;
import mg.lagrace.api.dto.system.RoleRequest;
import mg.lagrace.api.dto.system.UserRequest;
import mg.lagrace.api.dto.user.UserResponse;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.CalendarDay;
import mg.lagrace.api.models.Role;
import mg.lagrace.api.models.User;
import mg.lagrace.api.repositories.CalendarDayRepository;
import mg.lagrace.api.repositories.RoleRepository;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class SystemConfigService {
    private final CalendarDayRepository calendarDays;
    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public SystemConfigService(CalendarDayRepository calendarDays, RoleRepository roles,
            UserRepository users, PasswordEncoder passwordEncoder) {
        this.calendarDays = calendarDays; this.roles = roles; this.users = users; this.passwordEncoder = passwordEncoder;
    }

    public List<CalendarDay> getCalendarDays(LocalDate start, LocalDate end) {
        return start == null || end == null ? calendarDays.findAll() : calendarDays.findByDayBetween(start, end);
    }
    public CalendarDay updateCalendarDay(CalendarDayRequest request) {
        if (request == null || request.day() == null) throw new ValidationException("La date est obligatoire.");
        CalendarDay day = calendarDays.findById(request.day()).orElseGet(CalendarDay::new);
        day.setDay(request.day()); day.setHoliday(Boolean.TRUE.equals(request.holiday())); return calendarDays.save(day);
    }

    public List<Role> getRoles() { return roles.findAll(); }
    public Role addRole(RoleRequest request) { String name = name(request == null ? null : request.name(), "du rôle"); if (roles.existsByName(name)) duplicate(); Role role = new Role(); role.setName(name); role.setDescription(request.description()); return roles.save(role); }
    public Role updateRole(Long id, RoleRequest request) { Role role = find(roles, id, "Rôle"); String name = name(request == null ? null : request.name(), "du rôle"); if (!role.getName().equals(name) && roles.existsByName(name)) duplicate(); role.setName(name); role.setDescription(request.description()); return roles.save(role); }
    public void deleteRole(Long id) { roles.delete(find(roles, id, "Rôle")); }

    public List<UserResponse> getUsers() { return users.findAll().stream().map(this::response).toList(); }
    public UserResponse addUser(UserRequest request) { validateUser(request, true, null); User user = new User(); apply(user, request, true); return response(users.save(user)); }
    public UserResponse updateUser(Long id, UserRequest request) { User user = find(users, id, "Utilisateur"); validateUser(request, false, id); apply(user, request, false); return response(users.save(user)); }
    public void deleteUser(Long id) { users.delete(find(users, id, "Utilisateur")); }

    private void apply(User user, UserRequest r, boolean creating) {
        user.setLastName(name(r.lastName(), "du nom")); user.setFirstName(name(r.firstName(), "du prénom")); user.setBirthDate(r.birthDate()); user.setUsername(name(r.username(), "du nom d'utilisateur")); user.setEmail(blankToNull(r.email())); user.setActive(r.active() == null || r.active());
        if (creating || (r.password() != null && !r.password().isBlank())) user.setPassword(passwordEncoder.encode(r.password()));
        Set<Role> assigned = r.roleIds() == null ? Set.of() : r.roleIds().stream().map(id -> find(roles, id, "Rôle")).collect(Collectors.toSet()); user.setRoles(assigned);
    }
    private void validateUser(UserRequest r, boolean creating, Long id) {
        if (r == null) throw new ValidationException("Les données de l'utilisateur sont obligatoires.");
        name(r.lastName(), "du nom"); name(r.firstName(), "du prénom"); name(r.username(), "du nom d'utilisateur");
        if (creating && (r.password() == null || r.password().isBlank())) throw new ValidationException("Le mot de passe est obligatoire.");
        if (users.existsByUsername(r.username().trim())
                && (id == null || !users.findByUsername(r.username().trim()).map(u -> u.getIdUser().equals(id)).orElse(false))) duplicate();
        if (r.email() != null && !r.email().isBlank() && users.existsByEmail(r.email().trim())) {
            boolean unchanged = id != null && users.findById(id).map(u -> r.email().trim().equals(u.getEmail())).orElse(false);
            if (!unchanged) duplicate();
        }
    }
    private UserResponse response(User u) { return new UserResponse(u.getIdUser(), u.getLastName(), u.getFirstName(), u.getBirthDate(), u.getUsername(), u.getEmail(), u.isActive(), u.getRoles().stream().map(Role::getName).collect(Collectors.toSet())); }
    private String name(String value, String label) { if (value == null || value.isBlank()) throw new ValidationException("Le nom " + label + " est obligatoire."); return value.trim(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void duplicate() { throw new DuplicateResourceException("Cette valeur existe déjà."); }
    private <T> T find(org.springframework.data.jpa.repository.JpaRepository<T, Long> repo, Long id, String label) { if (id == null) throw new ValidationException("L'identifiant est obligatoire."); return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException(label + " introuvable avec l'id " + id)); }
}
