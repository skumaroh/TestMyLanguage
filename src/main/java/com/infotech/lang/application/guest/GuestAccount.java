package com.infotech.lang.application.guest;

import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * What marks a guest: an email of the form {@code guest-<random>@guest.invalid} (the {@code .invalid} domain is reserved and can never be a
 * real mailbox). Real accounts cannot use it: registration refuses the domain. The database clean-up function looks for the same form
 * and for the account type {@code GUEST}.
 */
public final class GuestAccount {

    public static final String DOMAIN = "@guest.invalid";
    public static final String PROVIDER = "GUEST";
    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private GuestAccount() {
    }

    /** A new random guest email, for example {@code guest-k3m9x2qa7bnr@guest.invalid}. */
    public static String newEmail() {
        StringBuilder id = new StringBuilder("guest-");
        for (int i = 0; i < 12; i++) {
            id.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return id + DOMAIN;
    }

    public static boolean isGuestEmail(String email) {
        if (email == null) {
            return false;
        }
        String e = email.trim().toLowerCase(Locale.ROOT);
        return e.startsWith("guest-") && e.endsWith(DOMAIN) && e.length() > "guest-".length() + DOMAIN.length();
    }

    /** True when the person signed in on this request is a guest. */
    public static boolean isGuestNow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && isGuestEmail(authentication.getName());
    }
}
