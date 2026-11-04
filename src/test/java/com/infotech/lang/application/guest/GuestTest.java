package com.infotech.lang.application.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infotech.lang.application.assessment.BlockEngine;
import org.junit.jupiter.api.Test;

class GuestTest {

    @Test
    void guestAddressesAreRecognisedAndRealOnesAreNot() {
        for (int i = 0; i < 50; i++) {
            String email = GuestAccount.newEmail();
            assertTrue(GuestAccount.isGuestEmail(email), email);
            assertTrue(email.matches("guest-[a-z2-9]{12}@guest\\.invalid"), email);
        }
        assertNotEquals(GuestAccount.newEmail(), GuestAccount.newEmail());
        assertTrue(GuestAccount.isGuestEmail("  GUEST-abc@Guest.Invalid "));
        assertFalse(GuestAccount.isGuestEmail("guest@example.com"));
        assertFalse(GuestAccount.isGuestEmail("guest-abc@gmail.com"));
        assertFalse(GuestAccount.isGuestEmail("someone@guest.invalid"));
        assertFalse(GuestAccount.isGuestEmail("guest-@guest.invalid"));
        assertFalse(GuestAccount.isGuestEmail(null));
    }

    @Test
    void aGuestModuleIsOneQuestion() {
        BlockEngine one = GuestMode.single(BlockEngine.PASS_RATIO);
        assertEquals(1, one.maxQuestions());
        assertFalse(one.isComplete());
        one.record(true);
        assertTrue(one.isComplete());
        assertTrue(one.passed());
        BlockEngine wrong = GuestMode.single(BlockEngine.PASS_RATIO);
        wrong.record(false);
        assertTrue(wrong.isComplete());
        assertFalse(wrong.passed());
    }

    @Test
    void aVisitorIsLimitedToTenGuestSessionsAnHour() {
        GuestThrottle throttle = new GuestThrottle();
        long now = 5_000_000L;
        for (int i = 0; i < GuestThrottle.MAX_STARTS; i++) {
            assertTrue(throttle.allow("1.2.3.4", now + i));
        }
        assertFalse(throttle.allow("1.2.3.4", now + 100));
        assertTrue(throttle.allow("5.6.7.8", now + 100));
        assertTrue(throttle.allow("1.2.3.4", now + GuestThrottle.WINDOW_MINUTES * 60_000L + 1_000));
    }
}
