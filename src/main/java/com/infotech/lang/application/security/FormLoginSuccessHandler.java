package com.infotech.lang.application.security;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.data.service.UserSessionLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Success handler for custom email/password form-based authentication.
 * Redirects users to the main application page after successful login.
 */
@Component
public class FormLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(FormLoginSuccessHandler.class);

    private final UserRepository userRepository;
    private final UserSessionLogService userSessionLogService;

    public FormLoginSuccessHandler(UserRepository userRepository, UserSessionLogService userSessionLogService) {
        this.userRepository = userRepository;
        this.userSessionLogService = userSessionLogService;
        // Set default target URL to the main page
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false); // Use saved request if available
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {

        String username = authentication.getName();
        logger.info("User logged in successfully: {}", username);

        // authentication.getName() is always the email - CustomUserDetailsService.loadUserByUsername
        // returns it as the principal's username regardless of whether the user typed their
        // userName, email, or ID to log in.
        userRepository.findByEmailIgnoreCase(username).ifPresent((User user) ->
                userSessionLogService.recordLogin(user, request.getSession().getId()));

        // Redirect to the saved request or default target
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
