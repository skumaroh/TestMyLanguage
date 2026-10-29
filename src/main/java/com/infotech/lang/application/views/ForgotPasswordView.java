package com.infotech.lang.application.views;

import com.infotech.lang.application.data.service.PasswordResetService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Forgot Password view - allows users to request password reset email.
 */
@Route("forgot-password")
@PageTitle("Forgot Password | Language Tool")
@AnonymousAllowed
public class ForgotPasswordView extends VerticalLayout {

    private final PasswordResetService passwordResetService;
    private final EmailField emailField = new EmailField("Email");
    private final Button submitButton = new Button("Send Reset Link");
    private final Button backToLoginButton = new Button("Back to Login");

    public ForgotPasswordView(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
        
        addClassName("forgot-password-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        getStyle().set("padding-top", "var(--lumo-space-xl)");
        
        createForgotPasswordLayout();
    }

    private void createForgotPasswordLayout() {
        H2 title = new H2("Reset Password");
        
        Paragraph instructions = new Paragraph(
            "Enter your email address and we'll send you a link to reset your password."
        );
        instructions.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        emailField.setPlaceholder("your@email.com");
        emailField.setWidthFull();
        emailField.setRequiredIndicatorVisible(true);
        
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();
        submitButton.addClickListener(e -> handlePasswordReset());
        
        backToLoginButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backToLoginButton.setWidthFull();
        backToLoginButton.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });
        
        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setMaxWidth("400px");
        formContainer.setPadding(true);
        formContainer.getStyle()
            .set("box-shadow", "var(--lumo-box-shadow-m)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("background", "var(--lumo-base-color)");
        
        formContainer.add(
            title,
            instructions,
            emailField,
            submitButton,
            backToLoginButton
        );
        
        add(formContainer);
    }

    private void handlePasswordReset() {
        String email = emailField.getValue();
        
        if (email == null || email.trim().isEmpty()) {
            showError("Please enter your email address");
            return;
        }
        
        if (!emailField.isInvalid() && isValidEmail(email)) {
            try {
                passwordResetService.initiatePasswordReset(email);
                showSuccess("If an account exists with that email, a password reset link has been sent.");
                
                // Clear form
                emailField.clear();
                
                // Redirect to login after 3 seconds
                submitButton.setEnabled(false);
                getUI().ifPresent(ui -> ui.access(() -> {
                    try {
                        Thread.sleep(3000);
                        ui.navigate(LoginView.class);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }));
            } catch (Exception ex) {
                showError("An error occurred. Please try again later.");
            }
        } else {
            showError("Please enter a valid email address");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
