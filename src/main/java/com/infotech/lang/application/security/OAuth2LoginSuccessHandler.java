package com.infotech.lang.application.security;

import com.infotech.lang.application.data.entity.Status;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.entity.UserRole;
import com.infotech.lang.application.data.repository.StatusRepository;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.data.service.UserSessionLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private final UserSessionLogService userSessionLogService;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, StatusRepository statusRepository,
            UserSessionLogService userSessionLogService) {
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.userSessionLogService = userSessionLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        // Extract user information based on provider
        String email = extractEmail(oAuth2User);
        String givenName = extractGivenName(oAuth2User);
        String familyName = extractFamilyName(oAuth2User);
        String provider = determineProvider(oAuth2User);
        
        // Check if user exists in database
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
        User resolvedUser;

        if (existingUser.isEmpty()) {
            // User doesn't exist, create new user
            User newUser = new User();
            newUser.setFirstName(givenName != null ? givenName : "");
            newUser.setLastName(familyName != null ? familyName : "");
            newUser.setEmail(email != null ? email : "");
            newUser.setDisplayName(resolveDisplayName(oAuth2User, givenName, email));

            // Set userName to email (required field for OAuth users)
            newUser.setUserName(email != null ? email : "");
            
            // Set auth provider (GOOGLE, FACEBOOK, or MICROSOFT)
            newUser.setAuthProvider(provider);
            
            // Set email as verified (coming from OAuth provider)
            newUser.setEmailVerified(true);
            
            // Password is null for OAuth users
            newUser.setPassword(null);

            newUser.setRole(UserRole.defaultRole());
            newUser.setEnabled(true);
            
            // Set default status (you may want to adjust this logic)
            // Assuming there's an "Active" status or you can get the first status
            Optional<Status> defaultStatus = statusRepository.findAll().stream().findFirst();
            if (defaultStatus.isPresent()) {
                newUser.setStatus(defaultStatus.get());
            }
            
            // Teacher can be null initially - user can be assigned a teacher later
            newUser.setTeacher(null);
            
            // Save the new user to database
            userRepository.save(newUser);
            resolvedUser = newUser;

            logger.info("New {} user created: {}", provider, email);
        } else {
            User existing = existingUser.get();
            boolean updated = false;
            if (existing.getRole() == null) {
                existing.setRole(UserRole.defaultRole());
                updated = true;
            }
            if (existing.getEnabled() == null) {
                existing.setEnabled(true);
                updated = true;
            }
            if (existing.getDisplayName() == null || existing.getDisplayName().isBlank()) {
                existing.setDisplayName(resolveDisplayName(oAuth2User, givenName, email));
                updated = true;
            }
            if (updated) {
                userRepository.save(existing);
            }
            resolvedUser = existing;
            logger.info("User already exists: {}", email);
        }

        userSessionLogService.recordLogin(resolvedUser, request.getSession().getId());

        // Continue with the default behavior (redirect to the original requested page or default success URL)
        super.onAuthenticationSuccess(request, response, authentication);
    }
    
    /**
     * Extract email from OAuth2User based on provider.
     */
    private String extractEmail(OAuth2User oAuth2User) {
        // Try standard email attribute first
        String email = oAuth2User.getAttribute("email");
        if (email != null) {
            return email;
        }
        
        // Microsoft sometimes uses "mail" or "userPrincipalName"
        email = oAuth2User.getAttribute("mail");
        if (email != null) {
            return email;
        }
        
        email = oAuth2User.getAttribute("userPrincipalName");
        return email;
    }
    
    /**
     * Extract first name from OAuth2User based on provider.
     */
    private String extractGivenName(OAuth2User oAuth2User) {
        // Google and Microsoft use given_name
        String givenName = oAuth2User.getAttribute("given_name");
        if (givenName != null) {
            return givenName;
        }
        
        // Facebook uses first_name
        givenName = oAuth2User.getAttribute("first_name");
        if (givenName != null) {
            return givenName;
        }
        
        // Fallback: try to split name
        String name = oAuth2User.getAttribute("name");
        if (name != null && name.contains(" ")) {
            return name.split(" ")[0];
        }
        
        return name;
    }
    
    /**
     * Extract last name from OAuth2User based on provider.
     */
    private String extractFamilyName(OAuth2User oAuth2User) {
        // Google and Microsoft use family_name
        String familyName = oAuth2User.getAttribute("family_name");
        if (familyName != null) {
            return familyName;
        }
        
        // Facebook uses last_name
        familyName = oAuth2User.getAttribute("last_name");
        if (familyName != null) {
            return familyName;
        }
        
        // Fallback: try to split name
        String name = oAuth2User.getAttribute("name");
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ");
            if (parts.length > 1) {
                return parts[parts.length - 1];
            }
        }
        
        return "";
    }
    
    private String resolveDisplayName(OAuth2User oAuth2User, String givenName, String email) {
        String name = oAuth2User.getAttribute("name");
        if (name != null && !name.isBlank()) return name.trim();
        if (givenName != null && !givenName.isBlank()) return givenName.trim();
        return (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')) : (email != null ? email : "");
    }

    /**
     * Determine the OAuth provider based on available attributes.
     */
    private String determineProvider(OAuth2User oAuth2User) {
        // Facebook has 'id' attribute that's numeric
        if (oAuth2User.getAttribute("id") != null && oAuth2User.getAttribute("first_name") != null) {
            return "FACEBOOK";
        }
        
        // Microsoft has 'userPrincipalName' or 'id' starting with specific patterns
        if (oAuth2User.getAttribute("userPrincipalName") != null || 
            oAuth2User.getAttribute("displayName") != null) {
            return "MICROSOFT";
        }
        
        // Default to Google
        return "GOOGLE";
    }
}
