package com.infotech.lang.application.views;

import com.infotech.lang.application.data.service.EmailVerificationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Map;

/**
 * Email Verification view - verifies user email using token from email link.
 */
@Route("verify-email")
@PageTitle("Email Verification | Language Tool")
@AnonymousAllowed
public class EmailVerificationView extends VerticalLayout implements BeforeEnterObserver {

    private final EmailVerificationService emailVerificationService;
    private final H2 title = new H2();
    private final Paragraph message = new Paragraph();
    private final Icon statusIcon = new Icon();
    private final Button actionButton = new Button();
    
    private String verificationToken;
    private boolean verificationSuccessful = false;

    public EmailVerificationView(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
        
        addClassName("email-verification-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        getStyle().set("padding-top", "var(--lumo-space-xl)");
        
        createVerificationLayout();
    }

    private void createVerificationLayout() {
        statusIcon.setSize("64px");
        statusIcon.getStyle()
            .set("margin-bottom", "var(--lumo-space-m)");
        
        title.getStyle().set("margin", "var(--lumo-space-s) 0");
        
        message.getStyle()
            .set("text-align", "center")
            .set("max-width", "400px");
        
        actionButton.setWidthFull();
        actionButton.setVisible(false);
        
        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("400px");
        container.setPadding(true);
        container.setAlignItems(Alignment.CENTER);
        container.getStyle()
            .set("box-shadow", "var(--lumo-box-shadow-m)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("background", "var(--lumo-base-color)");
        
        container.add(
            statusIcon,
            title,
            message,
            actionButton
        );
        
        add(container);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Extract token from URL query parameter
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        
        if (params.containsKey("token")) {
            verificationToken = params.get("token").get(0);
            verifyEmail();
        } else {
            showError("No verification token provided");
        }
    }

    private void verifyEmail() {
        try {
            boolean success = emailVerificationService.verifyEmail(verificationToken);
            
            if (success) {
                verificationSuccessful = true;
                showSuccess();
            } else {
                showError("Verification failed");
            }
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("An error occurred during verification");
        }
    }

    private void showSuccess() {
        statusIcon.setIcon(VaadinIcon.CHECK_CIRCLE);
        statusIcon.getStyle().set("color", "var(--lumo-success-color)");
        
        title.setText("Email Verified!");
        
        message.setText("Your email has been successfully verified. You can now log in to your account.");
        message.getStyle().set("color", "var(--lumo-success-text-color)");
        
        actionButton.setText("Go to Login");
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        actionButton.setVisible(true);
        actionButton.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });
        
        Notification notification = Notification.show(
            "Email verified successfully!", 
            3000, 
            Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        
        // Auto-redirect after 3 seconds
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                Thread.sleep(3000);
                ui.navigate(LoginView.class);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    private void showError(String errorMessage) {
        statusIcon.setIcon(VaadinIcon.CLOSE_CIRCLE);
        statusIcon.getStyle().set("color", "var(--lumo-error-color)");
        
        title.setText("Verification Failed");
        
        message.setText(errorMessage != null ? errorMessage : 
            "The verification link is invalid or has expired. Please request a new verification email.");
        message.getStyle().set("color", "var(--lumo-error-text-color)");
        
        actionButton.setText("Go to Login");
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionButton.setVisible(true);
        actionButton.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });
        
        Notification notification = Notification.show(
            "Email verification failed", 
            5000, 
            Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
