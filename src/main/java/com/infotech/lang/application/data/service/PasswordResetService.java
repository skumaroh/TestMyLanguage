package com.infotech.lang.application.data.service;

import com.infotech.lang.application.data.entity.PasswordResetToken;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.PasswordResetTokenRepository;
import com.infotech.lang.application.data.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling password reset functionality.
 */
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Initiate password reset by creating token and sending email.
     * 
     * @param email User's email address
     * @return true if email was sent, false if user not found
     */
    @Transactional
    public boolean initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
        
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists or not for security
            return true;
        }
        
        User user = userOpt.get();
        
        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);
        
        // Generate new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);
        
        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUserName(), token);
        
        return true;
    }

    /**
     * Validate a password reset token.
     * 
     * @param token The token string
     * @return Optional containing the token if valid, empty otherwise
     */
    public Optional<PasswordResetToken> validateToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        if (!resetToken.isValid()) {
            return Optional.empty();
        }
        
        return tokenOpt;
    }

    /**
     * Reset password using valid token.
     * 
     * @param token The password reset token
     * @param newPassword The new password (plain text, will be hashed)
     * @return true if password was reset successfully
     * @throws IllegalArgumentException if token is invalid or password is weak
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = validateToken(token);
        
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        
        // Send confirmation email
        emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), user.getUserName());
        
        return true;
    }

    /**
     * Clean up expired tokens (can be scheduled to run periodically).
     */
    @Transactional
    public void deleteExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
