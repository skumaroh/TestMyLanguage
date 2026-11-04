package com.infotech.lang.application.views;

import com.infotech.lang.application.certificate.CertificateNameService;
import com.infotech.lang.application.certificate.NameScreen;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.report.Certificate;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;

/** The learner's profile: the email, and the one "name on certificate", which can be saved once and not changed. */
@Route(value = "profile", layout = MainLayout.class)
@PageTitle("My profile | TestMyLanguage")
@PermitAll
public class ProfileView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final transient CertificateNameService names;
    private final transient NameScreen screen;
    private final transient User user;

    public ProfileView(SecurityService securityService, UserRepository userRepository, CertificateNameService names, NameScreen screen) {
        this.names = names;
        this.screen = screen;
        UserDetails details = securityService.getAuthenticatedUser();
        this.user = details == null ? null : userRepository.findByEmailIgnoreCase(details.getUsername()).orElse(null);
        setMaxWidth("720px");
        add(new H2("My profile"));
        if (user == null) {
            add(new Paragraph("Please sign in again."));
            return;
        }
        add(new Paragraph("Email: " + user.getEmail()));
        add(new H3("Name on certificate"));
        render();
    }

    private void render() {
        while (getComponentCount() > 3) {
            remove(getComponentAt(getComponentCount() - 1));
        }
        Optional<String> saved;
        try {
            saved = names.get(user.getId());
        } catch (RuntimeException e) {
            add(new Paragraph("Your certificate name could not be loaded right now. Please try again later."));
            return;
        }
        if (saved.isPresent()) {
            add(new Paragraph(saved.get()));
            add(new Paragraph("This name is saved and cannot be changed. If it has a mistake, ask an administrator to reset it."));
        } else {
            add(new CertificateNameForm(names, screen, user.getId(), Certificate.fullName(user.getFirstName(), user.getLastName()),
                    name -> render()));
        }
    }
}
