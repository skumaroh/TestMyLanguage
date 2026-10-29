package com.infotech.lang.application.security;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user by email for authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find by userName first (most likely login method)
        Optional<User> userOpt = userRepository.findByUserName(username);
        
        // If not found by userName, try to find by email
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailIgnoreCase(username);
        }
        
        // If not found by email, try to find by UUID ID
        if (userOpt.isEmpty()) {
            try {
                UUID userId = UUID.fromString(username);
                userOpt = userRepository.findById(userId);
            } catch (IllegalArgumentException e) {
                // Not a valid UUID, skip ID lookup
            }
        }
        
        User user = userOpt.orElseThrow(() -> 
            new UsernameNotFoundException("User not found with username, email, or ID: " + username));

        // Check if user has a password (custom auth user)
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new UsernameNotFoundException("User registered with OAuth provider. Please use Google Sign-In.");
        }
        
        // Check if email is verified — log a warning only; do not hard-block login
        // (Email verification is partially implemented and not yet proven in production)
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            org.slf4j.LoggerFactory.getLogger(CustomUserDetailsService.class)
                    .warn("User {} logged in with email_verified=false", user.getEmail());
        }

        if (!user.isAccountEnabled()) {
            throw new UsernameNotFoundException("Account disabled. Please contact an administrator or teacher.");
        }

        List<SimpleGrantedAuthority> authorities = user.getRoleOrDefault().getAuthorityNames().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        // Return Spring Security User with email as username
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
