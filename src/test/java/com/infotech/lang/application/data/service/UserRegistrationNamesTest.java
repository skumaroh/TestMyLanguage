package com.infotech.lang.application.data.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.infotech.lang.application.data.entity.Status;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.StatusRepository;
import com.infotech.lang.application.data.repository.TeacherRepository;
import com.infotech.lang.application.data.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

/** First and last name are optional at sign-up: they are asked for when a certificate is printed. */
class UserRegistrationNamesTest {

    private final UserRepository users = mock(UserRepository.class);
    private final StatusRepository statuses = mock(StatusRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserRegistrationService service = new UserRegistrationService(users, statuses, mock(TeacherRepository.class), encoder,
            mock(EmailVerificationService.class));

    private User register(String first, String last, String display) {
        when(encoder.encode(any())).thenReturn("hash");
        when(statuses.findByName("Not contacted")).thenReturn(Optional.of(new Status()));
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        service.registerUser(first, last, display, "asha_k", "asha@example.com", "secret1", null);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        Mockito.verify(users, Mockito.atLeastOnce()).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void namesCanBeLeftOutAndAreStoredAsNull() {
        User u = register("", "  ", null);
        assertNull(u.getFirstName());
        assertNull(u.getLastName());
    }

    @Test
    void providedNamesAreTrimmedAndTheFirstNameIsTheDefaultDisplayName() {
        User u = register(" Asha ", " Rao ", null);
        assertEquals("Asha", u.getFirstName());
        assertEquals("Rao", u.getLastName());
        assertEquals("Asha", u.getDisplayName());
        assertEquals("Ash", register("Asha", "Rao", " Ash ").getDisplayName());
    }

    @Test
    void usernameEmailAndPasswordAreStillRequiredAndNamesHaveALengthLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("A", "B", null, "", "asha@example.com", "secret1", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("x".repeat(101), "B", null, "asha_k", "asha@example.com", "secret1", null));
    }
}
