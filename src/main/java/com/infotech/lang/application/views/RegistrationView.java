package com.infotech.lang.application.views;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.service.UserRegistrationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Registration view for new users to sign up with email and password.
 */
@Route("register")
@PageTitle("Sign Up | Language Tool")
@AnonymousAllowed
public class RegistrationView extends VerticalLayout {

    private final UserRegistrationService registrationService;

    private TextField firstNameField;
    private TextField lastNameField;
    private TextField displayNameField;
    private TextField userNameField;
    private EmailField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private TextField teacherCodeField;
    private Button registerButton;
    private Span errorMessage;

    public RegistrationView(UserRegistrationService registrationService) {
        this.registrationService = registrationService;

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSizeFull();
        getStyle().set("padding-top", "var(--lumo-space-xl)");
        getStyle().set("overflow", "auto");

        createRegistrationForm();
    }

    private void createRegistrationForm() {
        H2 title = new H2("Create Account");
        
        firstNameField = new TextField("First Name");
        firstNameField.setRequired(true);
        firstNameField.setWidthFull();

        lastNameField = new TextField("Last Name");
        lastNameField.setRequired(true);
        lastNameField.setWidthFull();

        displayNameField = new TextField("Display Name");
        displayNameField.setPlaceholder("How should we address you? (optional)");
        displayNameField.setWidthFull();

        userNameField = new TextField("Username");
        userNameField.setRequired(true);
        userNameField.setPlaceholder("3+ characters (letters, numbers, -, _)");
        userNameField.setHelperText("Choose a unique username for login");
        userNameField.setWidthFull();

        emailField = new EmailField("Email");
        emailField.setRequired(true);
        emailField.setPlaceholder("name@example.com");
        emailField.setWidthFull();

        passwordField = new PasswordField("Password");
        passwordField.setRequired(true);
        passwordField.setHelperText("Minimum 6 characters");
        passwordField.setWidthFull();

        confirmPasswordField = new PasswordField("Confirm Password");
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setWidthFull();

        teacherCodeField = new TextField("Teacher Code");
        teacherCodeField.setPlaceholder("Optional — from your teacher");
        teacherCodeField.setHelperText("If your teacher gave you a code, enter it to link your account to them");
        teacherCodeField.setWidthFull();

        errorMessage = new Span();
        errorMessage.getStyle()
                .set("color", "var(--lumo-error-text-color)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("display", "none");

        registerButton = new Button("Sign Up");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        registerButton.addClickListener(e -> handleRegistration());

        // Login link
        RouterLink loginLink = new RouterLink("Already have an account? Sign in", LoginView.class);
        loginLink.getStyle().set("margin-top", "var(--lumo-space-m)");

        FormLayout formLayout = new FormLayout();
        formLayout.add(firstNameField, lastNameField, displayNameField, userNameField, emailField, passwordField,
                confirmPasswordField, teacherCodeField);
        formLayout.setMaxWidth("400px");
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setMaxWidth("400px");
        formContainer.setPadding(true);
        formContainer.getStyle()
            .set("box-shadow", "var(--lumo-box-shadow-m)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("background", "var(--lumo-base-color)")
            .set("margin", "var(--lumo-space-m)");
        formContainer.add(title, formLayout, errorMessage, registerButton, loginLink);

        add(formContainer);
    }

    private void handleRegistration() {
        // Clear previous error
        hideError();

        // Get values
        String firstName = firstNameField.getValue().trim();
        String lastName = lastNameField.getValue().trim();
        String displayName = displayNameField.getValue().trim();
        String userName = userNameField.getValue().trim();
        String email = emailField.getValue().trim();
        String password = passwordField.getValue();
        String confirmPassword = confirmPasswordField.getValue();
        String teacherCode = teacherCodeField.getValue().trim();

        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty() || userName.isEmpty() || email.isEmpty() || 
            password.isEmpty() || confirmPassword.isEmpty()) {
            showError("All fields are required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return;
        }

        try {
            // Register user
            User user = registrationService.registerUser(firstName, lastName, displayName, userName, email, password,
                    teacherCode);

            // Auto-login the user after successful registration
            authenticateUser(user);

            // Show success message
            Notification.show("Account created successfully! Welcome " + firstName + "!", 
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Redirect to main page
            getUI().ifPresent(ui -> ui.navigate(""));

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Registration failed. Please try again.");
            ex.printStackTrace();
        }
    }

    /**
     * Automatically authenticate the user after successful registration.
     */
    private void authenticateUser(User user) {
        org.springframework.security.core.userdetails.User userDetails = 
            new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoleOrDefault().getAuthorityNames().stream()
                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                        .toList()
            );
        
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.getStyle().set("display", "block");
    }

    private void hideError() {
        errorMessage.setText("");
        errorMessage.getStyle().set("display", "none");
    }
}
