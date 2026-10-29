package com.infotech.lang.application.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * Login page with both email/password authentication and Google OAuth.
 */
@Route("login")
@PageTitle("Login | Language Tool")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

	private static final String OAUTH_URL_GOOGLE = "/oauth2/authorization/google?prompt=select_account";
	private static final String OAUTH_URL_FACEBOOK = "/oauth2/authorization/facebook";
	private static final String OAUTH_URL_MICROSOFT = "/oauth2/authorization/microsoft";
	private final LoginForm loginForm = new LoginForm();
	private final Environment env;

	public LoginView(@Autowired Environment env) {
		this.env = env;
		
		addClassName("login-view");
		setSizeFull();
		setAlignItems(Alignment.CENTER);
		setJustifyContentMode(JustifyContentMode.START);
		getStyle().set("padding-top", "var(--lumo-space-xl)");
		getStyle().set("overflow", "auto");

		createLoginLayout();
	}

	private void createLoginLayout() {
		H2 title = new H2("Language Tool");
		
		// Email/Password Login Form
		loginForm.setAction("login");
		loginForm.setForgotPasswordButtonVisible(false); // We'll add custom link below
		
		// Get OAuth configuration
		String googleClientId = env.getProperty("spring.security.oauth2.client.registration.google.client-id");
		String facebookClientId = env.getProperty("spring.security.oauth2.client.registration.facebook.client-id");
		String microsoftClientId = env.getProperty("spring.security.oauth2.client.registration.microsoft.client-id");
		
		VerticalLayout loginContainer = new VerticalLayout();
		loginContainer.setMaxWidth("400px");
		loginContainer.setPadding(true);
		loginContainer.getStyle()
			.set("box-shadow", "var(--lumo-box-shadow-m)")
			.set("border-radius", "var(--lumo-border-radius-m)")
			.set("background", "var(--lumo-base-color)")
			.set("margin", "var(--lumo-space-m)");
		
		loginContainer.add(title);
		
		// Register link - at the top - MORE VISIBLE
		Paragraph registerText = new Paragraph("Don't have an account?");
		registerText.getStyle()
			.set("text-align", "center")
			.set("margin-top", "var(--lumo-space-l)")
			.set("margin-bottom", "var(--lumo-space-s)")
			.set("font-size", "var(--lumo-font-size-m)")
			.set("font-weight", "500");
		loginContainer.add(registerText);
		
		Button registerButton = new Button("Create Account");
		registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		registerButton.setWidthFull();
		registerButton.getStyle().set("margin-top", "var(--lumo-space-xs)");
		registerButton.addClickListener(e -> {
			getUI().ifPresent(ui -> ui.navigate(RegistrationView.class));
		});
		loginContainer.add(registerButton);
		
		// Divider - compact spacing
		Hr divider = new Hr();
		divider.getStyle().set("margin", "var(--lumo-space-s) 0");
		loginContainer.add(divider);
		
		Paragraph signInText = new Paragraph("Sign in with your email");
		signInText.getStyle().set("margin", "0");
		loginContainer.add(signInText);
		
		// Add the login form
		loginContainer.add(loginForm);
		
		// Forgot Password link
		Button forgotPasswordLink = new Button("Forgot Password?");
		forgotPasswordLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
		forgotPasswordLink.getStyle().set("margin-top", "var(--lumo-space-xs)");
		forgotPasswordLink.addClickListener(e -> {
			getUI().ifPresent(ui -> ui.navigate(ForgotPasswordView.class));
		});
		loginContainer.add(forgotPasswordLink);
		
		// OAuth buttons (if configured)
		boolean hasOAuth = false;
		
		if (isConfigured(googleClientId)) {
			if (!hasOAuth) {
				Paragraph orText = new Paragraph("Or sign in with");
				orText.getStyle().set("text-align", "center").set("margin", "0");
				loginContainer.add(orText);
				hasOAuth = true;
			}
			Button googleButton = createOAuthButton("Sign in with Google", () -> 
				getUI().ifPresent(ui -> ui.getPage().setLocation(OAUTH_URL_GOOGLE)));
			loginContainer.add(googleButton);
		}
		
		if (isConfigured(facebookClientId)) {
			if (!hasOAuth) {
				Paragraph orText = new Paragraph("Or sign in with");
				orText.getStyle().set("text-align", "center").set("margin", "0");
				loginContainer.add(orText);
				hasOAuth = true;
			}
			Button facebookButton = createOAuthButton("Sign in with Facebook", () -> 
				getUI().ifPresent(ui -> ui.getPage().setLocation(OAUTH_URL_FACEBOOK)));
			facebookButton.getElement().getStyle().set("background-color", "#1877F2");
			loginContainer.add(facebookButton);
		}
		
		if (isConfigured(microsoftClientId)) {
			if (!hasOAuth) {
				Paragraph orText = new Paragraph("Or sign in with");
				orText.getStyle().set("text-align", "center").set("margin", "0");
				loginContainer.add(orText);
				hasOAuth = true;
			}
			Button microsoftButton = createOAuthButton("Sign in with Microsoft", () -> 
				getUI().ifPresent(ui -> ui.getPage().setLocation(OAUTH_URL_MICROSOFT)));
			microsoftButton.getElement().getStyle().set("background-color", "#00A4EF");
			loginContainer.add(microsoftButton);
		}
		
		// Email and password sign-in is the default; OAuth buttons appear only when configured.
		
		add(loginContainer);
	}

	private Button createOAuthButton(String text, Runnable clickAction) {
		Button button = new Button(text);
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
		button.setWidthFull();
		button.getStyle().set("margin-top", "var(--lumo-space-xs)");
		button.getElement().addEventListener("click", e -> clickAction.run());
		return button;
	}
	
	private boolean isConfigured(String clientId) {
		return clientId != null && !clientId.isEmpty() && 
		       !clientId.contains("YOUR_") && clientId.length() >= 10;
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
		// Show error message if authentication failed
		if (beforeEnterEvent.getLocation()
				.getQueryParameters()
				.getParameters()
				.containsKey("error")) {
			loginForm.setError(true);
		}
	}
}

