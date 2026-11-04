package com.infotech.lang.application.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.data.entity.User;
import jakarta.validation.Validation;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A guest account must satisfy the same rules the user table enforces when it is saved (this failed once: an empty last name). */
class GuestAccountRulesTest {

    @Test
    void aGuestAccountPassesTheUserValidationRules() {
        User guest = GuestService.newGuestUser();
        var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(guest);
        assertTrue(violations.isEmpty(), () -> "violations: " + violations);
        assertTrue(GuestAccount.isGuestEmail(guest.getEmail()));
        assertEquals(guest.getEmail(), guest.getUserName());
        assertEquals(GuestAccount.PROVIDER, guest.getAuthProvider());
    }
}
