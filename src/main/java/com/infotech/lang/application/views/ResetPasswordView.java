package com.infotech.lang.application.views;

import com.infotech.lang.application.data.entity.PasswordResetToken;
import com.infotech.lang.application.data.service.PasswordResetService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reset Password view - allows users to reset password using token from email.
 */
@Route("reset-password")
@PageTitle("Reset Password | Language Tool")
@AnonymousAllowed
public class ResetPasswordView extends VerticalLayout implements BeforeEnterObserver {

    private final PasswordResetService passwordResetService;
    private final PasswordField newPasswordField = new PasswordField("New Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm Password");
    private final Button submitButton = new Button("Reset Password");
    private final Button backToLoginButton = new Button("Back to Login");
    private final Paragraph errorMessage = new Paragraph();
    
    private String resetToken;

    public ResetPasswordView(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
        
        addClassName("reset-password-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        getStyle().set("padding-top", "var(--lumo-space-xl)");
        
        createResetPasswordLayout();
    }

    private void createResetPasswordLayout() {
        H2 title = new H2("Reset Your Password");
        
        Paragraph instructions = new Paragraph(
            "Enter your new password below. Password must be at least 6 characters long."
        );
        instructions.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        errorMessage.getStyle()
            .set("color", "var(--lumo-error-text-color)")
            .set("display", "none");
        
        newPasswordField.setPlaceholder("Enter new password");
        newPasswordField.setWidthFull();
        newPasswordField.setRequiredIndicatorVisible(true);
        newPasswordField.setMinLength(6);
        newPasswordField.setHelperText("At least 6 characters");
        
        confirmPasswordField.setPlaceholder("Confirm new password");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequiredIndicatorVisible(true);
        
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidthFull();
        submitButton.addClickListener(e -> handleResetPassword());
        
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
            errorMessage,
            newPasswordField,
            confirmPasswordField,
            submitButton,
            backToLoginButton
        );
        
        add(formContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Extract token from URL query parameter
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        
        if (params.containsKey("token")) {
            resetToken = params.get("token").get(0);
            
            // Validate token
            Optional<PasswordResetToken> tokenOpt = passwordResetService.validateToken(resetToken);
            
            if (tokenOpt.isEmpty()) {
                showErrorMessage("Invalid or expired reset link. Please request a new password reset.");
                submitButton.setEnabled(false);
            }
        } else {
            showErrorMessage("No reset token provided. Please use the link from your email.");
            submitButton.setEnabled(false);
        }
    }

    private void handleResetPassword() {
        String newPassword = newPasswordField.getValue();
        String confirmPassword = confirmPasswordField.getValue();
        
        // Validation
        if (newPassword == null || newPassword.isEmpty()) {
            showError("Please enter a new password");
            return;
        }
        
        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters long");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        try {
            boolean success = passwordResetService.resetPassword(resetToken, newPassword);
            
            if (success) {
                showSuccess("Password reset successful! Redirecting to login...");
                
                // Disable form
                submitButton.setEnabled(false);
                newPasswordField.setEnabled(false);
                confirmPasswordField.setEnabled(false);
                
                // Redirect to login after 2 seconds
                getUI().ifPresent(ui -> ui.access(() -> {
                    try {
                        Thread.sleep(2000);
                        ui.navigate(LoginView.class);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("An error occurred. Please try again later.");
        }
    }

    private void showErrorMessage(String message) {
        errorMessage.setText(message);
        errorMessage.getStyle().set("display", "block");
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
