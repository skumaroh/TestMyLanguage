package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessmentService;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Admin page to try any one assessment block directly. Each button starts a fresh test attempt (the admin's earlier
 * unfinished attempt is marked incomplete), so a block can be run again and again without going through the earlier blocks.
 */
@Route(value = "admin/blocks", layout = MainLayout.class)
@PageTitle("Test one block | TestMyLanguage")
@RolesAllowed("ADMIN")
public class AdminBlocksView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final int LEVEL = 1;

    public AdminBlocksView(SecurityService securityService, UserRepository userRepository,
                           AssessmentService assessment, LanguageContext languageContext) {
        add(new H2("Test one block"));
        add(new Paragraph("Each button starts a fresh test attempt and opens that block. "
                + "At the end of a block you can still continue to the next ones. "
                + "Test runs are saved like real ones, under your own account."));
        addBlock("1. Words", "assessment/words", securityService, userRepository, assessment, languageContext, false);
        addBlock("2. Draw letters", "assessment/letters", securityService, userRepository, assessment, languageContext, true);
        addBlock("3. Audio words", "assessment/audio-words", securityService, userRepository, assessment, languageContext, true);
        addBlock("4. Translation", "assessment/translation", securityService, userRepository, assessment, languageContext, true);
    }

    private void addBlock(String label, String route, SecurityService securityService, UserRepository userRepository,
                          AssessmentService assessment, LanguageContext languageContext, boolean startAttemptFirst) {
        add(new Button(label, e -> {
            try {
                // the words block starts its own attempt; the others join the current one, so give them a clean one
                if (startAttemptFirst) {
                    UserDetails user = securityService.getAuthenticatedUser();
                    UUID userId = user == null ? null
                            : userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
                    if (userId == null) {
                        Notification.show("Please sign in again.");
                        return;
                    }
                    assessment.startAttempt(userId, languageContext.getLanguage(), LEVEL);
                }
                getUI().ifPresent(ui -> ui.navigate(route));
            } catch (RuntimeException ex) {
                Notification.show("Could not start the test attempt.");
            }
        }));
    }
}
