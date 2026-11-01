package com.infotech.lang.application.views;

import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import org.springframework.security.core.context.SecurityContextHolder;

/** Page frame for signed-in pages: a hamburger menu with the main links, the admin links (admins only) and sign out. */
public class MainLayout extends AppLayout {

    private static final long serialVersionUID = 1L;

    public MainLayout(SecurityService securityService) {
        H1 title = new H1("TestMyLanguage");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");
        addToNavbar(new DrawerToggle(), title);

        VerticalLayout menu = new VerticalLayout();
        menu.setPadding(true);
        menu.add(new RouterLink("Home", HomeView.class));
        menu.add(new RouterLink("Start the assessment", WelcomeView.class));
        if (isAdmin()) {
            menu.add(new RouterLink("Test Blocks", AdminBlocksView.class));
            menu.add(new RouterLink("Admin: conversations", ConversationAdminView.class));
            menu.add(new RouterLink("Admin: proctor texts", ProctorReviewView.class));
            menu.add(new RouterLink("Admin: handwriting check", HandwritingCheckView.class));
            menu.add(new RouterLink("Admin: translation check", TranslationCheckView.class));
        }
        Button signOut = new Button("Sign out", e -> securityService.logout());
        signOut.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        menu.add(signOut);
        addToDrawer(menu);
        setPrimarySection(Section.NAVBAR);
    }

    static boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
