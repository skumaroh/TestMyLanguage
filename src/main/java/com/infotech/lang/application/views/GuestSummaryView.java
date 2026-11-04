package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessAttemptRepository;
import com.infotech.lang.application.assessment.AssessBlockResult;
import com.infotech.lang.application.assessment.AssessBlockResultRepository;
import com.infotech.lang.application.assessment.Modules;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.guest.GuestAccount;
import com.infotech.lang.application.guest.GuestService;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import jakarta.annotation.security.PermitAll;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/** What a guest sees at the end: how each of their questions went, and the way to the full assessment. Not a certificate or a report. */
@Route(value = "assessment/guest-summary", layout = MainLayout.class)
@PageTitle("Your quick summary | TestMyLanguage")
@PermitAll
public class GuestSummaryView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final String GOOGLE_URL = "/oauth2/authorization/google?prompt=select_account";

    private final transient GuestService guests;
    private final String language;

    public GuestSummaryView(SecurityService securityService, UserRepository userRepository, AssessAttemptRepository attempts,
                            AssessBlockResultRepository blocks, GuestService guests, LanguageContext languageContext) {
        this.guests = guests;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("640px");
        getStyle().set("margin", "0 auto");
        UserDetails user = securityService.getAuthenticatedUser();
        if (user == null || !GuestAccount.isGuestEmail(user.getUsername())) {
            add(new H2("Quick summary"), new Paragraph("This page is for guests. Sign in to see your own results."));
            return;
        }
        UUID userId = userRepository.findByEmailIgnoreCase(user.getUsername()).map(u -> u.getId()).orElse(null);
        Map<String, AssessBlockResult> done = userId == null ? Map.of()
                : attempts.findFirstByUserIdAndStatusOrderByIdDesc(userId, "IN_PROGRESS")
                        .map(a -> blocks.findByAttemptIdOrderByBlockOrder(a.getId()).stream().filter(b -> b.getFinishedAt() != null)
                                .collect(Collectors.toMap(AssessBlockResult::getBlockCode, b -> b, (x, y) -> y)))
                        .orElse(Map.of());
        add(new H2("Your quick summary"));
        add(new Paragraph("One question in each part. This is only a taste: the full assessment asks up to 8 per part, writes a "
                + "personal report, and gives a certificate for 90% and above."));
        for (Modules module : Modules.values()) {
            AssessBlockResult result = done.get(module.code());
            if (result == null) {
                continue;
            }
            boolean right = result.getCorrect() >= 1;
            Paragraph line = new Paragraph((right ? "✓ " : "✗ ") + module.heading() + (right ? ": right" : ": not this time"));
            line.getStyle().set("color", right ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)").set("margin", "0.2em 0");
            add(line);
        }
        // counted once per guest session
        var session = VaadinServletRequest.getCurrent().getHttpServletRequest().getSession();
        if (session.getAttribute("guestFinishedCounted") == null) {
            session.setAttribute("guestFinishedCounted", Boolean.TRUE);
            guests.event(language, GuestService.FINISHED, null);
        }
        add(new Paragraph("Want the full assessment, your own report and a certificate? Sign in or create a free account."));
        Button google = new Button("Sign in with Google", e -> leave(GuestService.SIGNIN_GOOGLE, GOOGLE_URL));
        google.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button register = new Button("Create an account", e -> leave(GuestService.SIGNIN_REGISTER, "/register"));
        Button login = new Button("I already have an account", e -> leave(null, "/login"));
        login.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(google, register, login);
    }

    /** Ends the guest session (its data is deleted later) and goes to the chosen sign-in page. */
    private void leave(String event, String url) {
        if (event != null) {
            guests.event(language, event, null);
        }
        var request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        new SecurityContextLogoutHandler().logout(request, null, null);
        UI.getCurrent().getPage().setLocation(url);
    }
}
