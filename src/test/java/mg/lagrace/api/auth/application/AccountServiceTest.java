package mg.lagrace.api.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.dto.user.AccountProfileRequest;
import mg.lagrace.api.auth.dto.user.ChangePasswordRequest;
import mg.lagrace.api.auth.persistence.UserRepository;
import mg.lagrace.api.employee.Employee;
import mg.lagrace.api.employee.persistence.EmployeeRepository;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.media.application.PhotoService;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock private UserRepository users;
    @Mock private EmployeeRepository employees;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PhotoService photos;

    private AccountService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AccountService(users, employees, passwordEncoder, photos);
        user = new User();
        user.setIdUser(15L);
        user.setUsername("account-owner");
        user.setFirstName("Ancien");
        user.setLastName("Nom");
        user.setEmail("old@example.com");
        user.setPassword("existing-hash");
    }

    @Test
    void profileUpdateChangesOnlyTheAuthenticatedUsersEditableFields() {
        when(users.findByUsername("account-owner")).thenReturn(Optional.of(user));
        when(users.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(employees.findByUserIdUser(15L)).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(photos.listByItem(any(), any())).thenReturn(java.util.List.of());

        service.updateProfile("account-owner",
                new AccountProfileRequest("  Nouveau  ", "  Nom  ", null, " new@example.com ", null, null));

        assertEquals("Nouveau", user.getFirstName());
        assertEquals("Nom", user.getLastName());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("account-owner", user.getUsername());
        verify(users).save(user);
    }

    @Test
    void profileUpdateAlsoUpdatesPersonalContactFieldsOnLinkedEmployee() {
        Employee employee = new Employee();
        when(users.findByUsername("account-owner")).thenReturn(Optional.of(user));
        when(employees.findByUserIdUser(15L)).thenReturn(Optional.of(employee));
        when(photos.listByItem(any(), any())).thenReturn(java.util.List.of());
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProfile("account-owner",
                new AccountProfileRequest("Nouveau", "Nom", null, null, " 0340000000 ", " Antsirabe "));

        assertEquals("Nouveau", employee.getFirstName());
        assertEquals("Nom", employee.getLastName());
        assertEquals("0340000000", employee.getPhone());
        assertEquals("Antsirabe", employee.getAddress());
    }

    @Test
    void passwordChangeRejectsAnIncorrectCurrentPassword() {
        when(users.findByUsername("account-owner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "existing-hash")).thenReturn(false);

        assertThrows(ValidationException.class, () -> service.changePassword(
                "account-owner",
                new ChangePasswordRequest("wrong-password", "new-password", "new-password")));

        verify(users, never()).save(any(User.class));
    }

    @Test
    void passwordChangeStoresOnlyAnEncodedNewPassword() {
        when(users.findByUsername("account-owner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "existing-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "existing-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.changePassword("account-owner",
                new ChangePasswordRequest("current-password", "new-password", "new-password"));

        assertEquals("new-hash", user.getPassword());
        verify(users).save(user);
    }

    @Test
    void passwordChangeRejectsMismatchedConfirmation() {
        assertThrows(ValidationException.class, () -> service.changePassword(
                "account-owner",
                new ChangePasswordRequest("current-password", "new-password", "different-password")));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(users, never()).save(any(User.class));
    }
}
