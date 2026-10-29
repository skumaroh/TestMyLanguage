package com.infotech.lang.application.data.service;

import com.infotech.lang.application.data.entity.EmailVerificationToken;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.EmailVerificationTokenRepository;
import com.infotech.lang.application.data.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling email verification functionality.
 */
@Service
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /**
     * Send verification email to user.
     * 
     * @param user The user to send verification email to
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);
        
        // Generate new token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken(token, user);
        tokenRepository.save(verificationToken);
        
        // Send email
        emailService.sendEmailVerificationEmail(user.getEmail(), user.getUserName(), token);
    }

    /**
     * Resend verification email to user by email address.
     * 
     * @param email User's email address
     * @return true if email was sent, false if user not found or already verified
     */
    @Transactional
    public boolean resendVerificationEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
        
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // Check if already verified
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return false;
        }
        
        sendVerificationEmail(user);
        return true;
    }

    /**
     * Validate an email verification token.
     * 
     * @param token The token string
     * @return Optional containing the token if valid, empty otherwise
     */
    public Optional<EmailVerificationToken> validateToken(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }
        
        EmailVerificationToken verificationToken = tokenOpt.get();
        
        if (!verificationToken.isValid()) {
            return Optional.empty();
        }
        
        return tokenOpt;
    }

    /**
     * Verify user's email using token.
     * 
     * @param token The verification token
     * @return true if email was verified successfully
     * @throws IllegalArgumentException if token is invalid or expired
     */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<EmailVerificationToken> tokenOpt = validateToken(token);
        
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }
        
        EmailVerificationToken verificationToken = tokenOpt.get();
        User user = verificationToken.getUser();
        
        // Mark email as verified
        user.setEmailVerified(true);
        userRepository.save(user);
        
        // Mark token as used
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
        
        return true;
    }

    /**
     * Check if user's email is verified.
     * 
     * @param user The user to check
     * @return true if email is verified
     */
    public boolean isEmailVerified(User user) {
        return Boolean.TRUE.equals(user.getEmailVerified());
    }

    /**
     * Clean up expired tokens (can be scheduled to run periodically).
     */
    @Transactional
    public void deleteExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
