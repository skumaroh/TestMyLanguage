package com.infotech.lang.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.entity.UserRole;
import com.infotech.lang.application.data.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleOidcUserServiceTest {

    private final UserRepository users = mock(UserRepository.class);

    private static OidcUser googleUser(String email, Object verified) {
        Map<String, Object> claims = new java.util.HashMap<>(Map.of("sub", "123", "email", email));
        if (verified != null) {
            claims.put("email_verified", verified);
        }
        OidcIdToken token = new OidcIdToken("t", Instant.now(), Instant.now().plusSeconds(60), claims);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
    }

    private GoogleOidcUserService service(OidcUser fromGoogle) {
        return new GoogleOidcUserService(request -> fromGoogle, users);
    }

    @Test
    void aNewPersonSignsInAsAStudentUsingTheirEmail() {
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        OidcUser user = service(googleUser("new@example.com", true)).loadUser(mock(OidcUserRequest.class));
        assertTrue(user instanceof GoogleLoginUser);
        assertEquals("new@example.com", ((GoogleLoginUser) user).getUsername());
        List<String> names = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(names.contains("ROLE_USER"));
        assertTrue(!names.contains("ROLE_ADMIN"));
    }

    @Test
    void anExistingAdminKeepsTheAdminRole() {
        User admin = new User();
        admin.setEmail("boss@example.com");
        admin.setRole(UserRole.ADMIN);
        when(users.findByEmailIgnoreCase("Boss@Example.com")).thenReturn(Optional.of(admin));
        OidcUser user = service(googleUser("Boss@Example.com", true)).loadUser(mock(OidcUserRequest.class));
        assertEquals("boss@example.com", ((GoogleLoginUser) user).getUsername());
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void anAddressGoogleHasNotVerifiedIsRefused() {
        assertThrows(OAuth2AuthenticationException.class, () -> service(googleUser("x@example.com", false)).loadUser(mock(OidcUserRequest.class)));
        assertThrows(OAuth2AuthenticationException.class, () -> service(googleUser("x@example.com", null)).loadUser(mock(OidcUserRequest.class)));
    }

    @Test
    void aDisabledAccountIsRefused() {
        User off = new User();
        off.setEmail("off@example.com");
        off.setEnabled(false);
        when(users.findByEmailIgnoreCase("off@example.com")).thenReturn(Optional.of(off));
        assertThrows(OAuth2AuthenticationException.class, () -> service(googleUser("off@example.com", true)).loadUser(mock(OidcUserRequest.class)));
    }
}
