package com.infotech.lang.application.security;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.entity.UserRole;
import com.infotech.lang.application.data.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Turns the person Google tells us about into our signed-in user. It only lets in an email address that Google says it has verified,
 * refuses a disabled account, and takes the roles from our database (a new person gets the student role).
 */
@Component
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> google;
    private final UserRepository users;

    public GoogleOidcUserService(UserRepository users) {
        this(new OidcUserService(), users);
    }

    GoogleOidcUserService(OAuth2UserService<OidcUserRequest, OidcUser> google, UserRepository users) {
        this.google = google;
        this.users = users;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser fromGoogle = google.loadUser(request);
        String email = fromGoogle.getEmail();
        if (email == null || email.isBlank() || !Boolean.TRUE.equals(fromGoogle.getEmailVerified())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_verified"), "Google did not give a verified email address");
        }
        email = email.trim();
        Optional<User> known = users.findByEmailIgnoreCase(email);
        if (known.isPresent() && !known.get().isAccountEnabled()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_disabled"), "Account disabled");
        }
        UserRole role = known.map(User::getRoleOrDefault).orElse(UserRole.defaultRole());
        List<SimpleGrantedAuthority> authorities = role.getAuthorityNames().stream().map(SimpleGrantedAuthority::new).toList();
        // the email as stored when the person already exists, so the sign-in matches the account exactly
        String username = known.map(User::getEmail).filter(e -> e != null && !e.isBlank()).orElse(email);
        return new GoogleLoginUser(fromGoogle, username, authorities);
    }
}
