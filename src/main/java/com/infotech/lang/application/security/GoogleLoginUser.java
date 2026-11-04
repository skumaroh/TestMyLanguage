package com.infotech.lang.application.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The signed-in person when they used Google: an OpenID Connect user that is also a {@link UserDetails} whose username is their email,
 * exactly like a password sign-in, so the rest of the application (which finds the signed-in student by email) needs no special case.
 * The authorities come from the student's role in our database, not from Google.
 */
public final class GoogleLoginUser implements OidcUser, UserDetails {

    private static final long serialVersionUID = 1L;

    private final transient OidcUser google;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public GoogleLoginUser(OidcUser google, String email, Collection<? extends GrantedAuthority> authorities) {
        this.google = google;
        this.email = email;
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getName() {
        return email;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Map<String, Object> getClaims() {
        return google.getClaims();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return google.getAttributes();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return google.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return google.getIdToken();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
