package com.infotech.lang.application.data.service;

import com.infotech.lang.application.data.entity.Status;
import com.infotech.lang.application.data.entity.Teacher;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.entity.UserRole;
import com.infotech.lang.application.data.repository.StatusRepository;
import com.infotech.lang.application.data.repository.TeacherRepository;
import com.infotech.lang.application.data.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user registration with password hashing and validation.
 */
@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public UserRegistrationService(UserRepository userRepository,
                                  StatusRepository statusRepository,
                                  TeacherRepository teacherRepository,
                                  PasswordEncoder passwordEncoder,
                                  EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Register a new user with email, userName, and password.
     *
     * @param firstName User's first name
     * @param lastName User's last name
     * @param userName Unique username (created by user)
     * @param email User's email address
     * @param password Plain text password (will be hashed)
     * @param teacherCode Optional code shared by a teacher; when non-blank, must match a
     *        Teacher.studentCode or registration fails — links the new user to that teacher
     * @return The registered user
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public User registerUser(String firstName, String lastName, String displayName, String userName, String email,
            String password, String teacherCode) {
        // Validate input
        validateRegistrationInput(firstName, lastName, userName, email, password);

        // Check if email already exists
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email address is already registered");
        }

        // Check if userName already exists
        if (userRepository.existsByUserName(userName)) {
            throw new IllegalArgumentException("Username is already taken. Please choose a different one");
        }

        // Resolve the optional teacher code before creating the user, so a bad code fails
        // registration outright rather than silently registering with no teacher linkage.
        Teacher teacher = null;
        if (teacherCode != null && !teacherCode.isBlank()) {
            teacher = teacherRepository.findByStudentCodeIgnoreCase(teacherCode.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Teacher code not recognized. Please check with your teacher."));
        }

        // Create new user
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setDisplayName((displayName != null && !displayName.isBlank()) ? displayName.trim() : firstName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setUserName(userName.trim());
        
        // Hash password
        user.setPassword(passwordEncoder.encode(password));
        
        // Set auth provider
        user.setAuthProvider("LOCAL");

        user.setRole(UserRole.defaultRole());
        user.setEnabled(true);
        
        // Set email as NOT verified - user must verify email before login
        user.setEmailVerified(false);
        
        // Set default status to "Not contacted" (first step in contact lifecycle)
        Status defaultStatus = statusRepository.findByName("Not contacted")
                .orElseGet(() -> statusRepository.findAll().stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No status found in database")));
        user.setStatus(defaultStatus);
        
        // Teacher is null unless a valid teacher code was provided above
        user.setTeacher(teacher);
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Attempt to send verification email — failure must not roll back the registration
        try {
            emailVerificationService.sendVerificationEmail(savedUser);
        } catch (Exception e) {
            // Log and continue; user is saved and can request a resend later
            org.slf4j.LoggerFactory.getLogger(UserRegistrationService.class)
                    .warn("Verification email failed for {}. User was still registered. Cause: {}",
                            savedUser.getEmail(), e.getMessage());
        }
        
        return savedUser;
    }

    /**
     * Validate registration input fields.
     */
    private void validateRegistrationInput(String firstName, String lastName, String userName, String email, String password) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (userName.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (!userName.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, underscores, and hyphens");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
    }

    /**
     * Simple email validation.
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
