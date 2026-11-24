package mg.lagrace.api.auth.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.dto.user.AccountProfileRequest;
import mg.lagrace.api.auth.dto.user.AccountResponse;
import mg.lagrace.api.auth.dto.user.ChangePasswordRequest;
import mg.lagrace.api.auth.persistence.UserRepository;
import mg.lagrace.api.employee.Employee;
import mg.lagrace.api.employee.persistence.EmployeeRepository;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.media.PhotoItemType;
import mg.lagrace.api.media.application.PhotoService;
import mg.lagrace.api.media.dto.photo.PhotoResponse;

@Service
public class AccountService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;
    private final PhotoService photos;

    public AccountService(UserRepository users, EmployeeRepository employees,
            PasswordEncoder passwordEncoder, PhotoService photos) {
        this.users = users;
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.photos = photos;
    }

    @Transactional(readOnly = true)
    public AccountResponse getCurrentAccount(String username) {
        return response(findCurrentUser(username));
    }

    @Transactional
    public AccountResponse updateProfile(String username, AccountProfileRequest request) {
        if (request == null) {
            throw new ValidationException("Les informations du profil sont obligatoires.");
        }

        User user = findCurrentUser(username);
        String firstName = required(request.firstName(), "Le prénom");
        String lastName = required(request.lastName(), "Le nom");
        String email = blankToNull(request.email());

        if (request.birthDate() != null && request.birthDate().isAfter(LocalDate.now())) {
            throw new ValidationException("La date de naissance ne peut pas être dans le futur.");
        }
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("L'adresse e-mail n'est pas valide.", "email");
        }
        if (email != null) {
            users.findByEmail(email)
                    .filter(existing -> !existing.getIdUser().equals(user.getIdUser()))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException("Cette adresse e-mail est déjà utilisée.");
                    });
        }

        var linkedEmployee = employees.findByUserIdUser(user.getIdUser());
        if (linkedEmployee.isPresent() && (firstName.length() > 100 || lastName.length() > 100)) {
            throw new ValidationException("Le prénom et le nom de votre fiche employé ne peuvent pas dépasser 100 caractères.");
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBirthDate(request.birthDate());
        user.setEmail(email);
        linkedEmployee.ifPresent(employee -> {
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setDateOfBirth(request.birthDate());
            employee.setPhone(blankToNull(request.phone()));
            employee.setAddress(blankToNull(request.address()));
        });
        return response(users.save(user));
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (request == null) {
            throw new ValidationException("Les informations de changement de mot de passe sont obligatoires.");
        }
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ValidationException("La confirmation du nouveau mot de passe ne correspond pas.", "confirmNewPassword");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Le nouveau mot de passe dépasse la longueur maximale autorisée.");
        }

        User user = findCurrentUser(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ValidationException("Le mot de passe actuel est incorrect.", "currentPassword");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ValidationException("Le nouveau mot de passe doit être différent de l'ancien.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
    }

    @Transactional
    public AccountResponse uploadProfilePhoto(String username, MultipartFile file) throws IOException {
        User user = findCurrentUser(username);
        photos.upload(PhotoItemType.USER, user.getIdUser(), file, "Photo de profil",
                "Photo de profil de " + user.getFirstName() + " " + user.getLastName(), true);
        return response(user);
    }

    private AccountResponse response(User user) {
        List<PhotoResponse> userPhotos = photos.listByItem(PhotoItemType.USER, user.getIdUser());
        String photoUrl = userPhotos.stream()
                .filter(PhotoResponse::cover)
                .findFirst()
                .or(() -> userPhotos.stream().findFirst())
                .map(PhotoResponse::url)
                .orElse(null);

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .sorted()
                .toList();
        var employee = employees.findByUserIdUser(user.getIdUser());
        LocalDate birthDate = user.getBirthDate() != null
                ? user.getBirthDate()
                : employee.map(Employee::getDateOfBirth).orElse(null);
        return new AccountResponse(user.getIdUser(), user.getFirstName(), user.getLastName(),
                birthDate, user.getUsername(), user.getEmail(), employee.isPresent(),
                employee.map(Employee::getPhone).orElse(null), employee.map(Employee::getAddress).orElse(null),
                roles, photoUrl);
    }

    private User findCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("L'utilisateur authentifié est introuvable.");
        }
        return users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(label + " est obligatoire.");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
