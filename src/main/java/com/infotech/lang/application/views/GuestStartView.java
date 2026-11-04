package com.infotech.lang.application.views;

import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.guest.GuestService;
import com.infotech.lang.application.guest.GuestThrottle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * "Try it as a guest": one question per module, no sign-up. Makes a temporary guest account (no personal data), signs the visitor in as
 * that account and opens the home page. Switched off unless {@code guest.enabled} is true.
 */
@Route("guest")
@PageTitle("Try as a guest | TestMyLanguage")
@AnonymousAllowed
public class GuestStartView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GuestStartView.class);

    private final transient GuestService guests;
    private final transient GuestThrottle throttle;
    private final String language;

    public GuestStartView(GuestService guests, GuestThrottle throttle, LanguageContext languageContext) {
        this.guests = guests;
        this.throttle = throttle;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("640px");
        getStyle().set("margin", "0 auto");
        add(new H1("Try it as a guest"));
        if (!guests.enabled()) {
            add(new Paragraph("Guest access is not available right now."), new RouterLink("Sign in", LoginView.class));
            return;
        }
        add(new Paragraph("See a sample of the " + languageContext.getDisplayName() + " assessment: a few quick questions, "
                + "about two minutes, no sign-up."));
        add(new Paragraph("Guest sessions are deleted after 24 hours. Your answers are checked by Meta AI. "
                + "Sign in afterwards for the full assessment and a certificate."));
        Button start = new Button("Start as a guest", e -> start());
        start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(start, new RouterLink("I already have an account: sign in", LoginView.class));
    }

    private void start() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (!throttle.allow(visitor(request), System.currentTimeMillis())) {
            Notification.show("Too many guest sessions from your connection just now. Please try again later, or sign in.");
            return;
        }
        try {
            HttpServletRequest http = ((VaadinServletRequest) request).getHttpServletRequest();
            http.getSession(true);
            http.changeSessionId();
            User guest = guests.createGuest(language, http.getSession().getId());
            List<GrantedAuthority> authorities = guest.getRoleOrDefault().getAuthorityNames().stream()
                    .map(name -> (GrantedAuthority) new SimpleGrantedAuthority(name)).toList();
            UserDetails principal = org.springframework.security.core.userdetails.User.withUsername(guest.getEmail())
                    .password("{noop}no-password-sign-in").authorities(authorities).build();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
            SecurityContextHolder.setContext(context);
            http.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            UI.getCurrent().getPage().setLocation("/");
        } catch (RuntimeException e) {
            logger.error("Could not start a guest session", e);
            Notification.show("Sorry, guest access could not start. Please sign in instead.");
        }
    }

    /** The first address in X-Forwarded-For (set by the ingress), else the connection's address. Kept in memory only. */
    static String visitor(VaadinRequest request) {
        if (request == null) {
            return "?";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
