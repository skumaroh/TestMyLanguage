package com.infotech.lang.application.views;

import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.service.AzureBlobService;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Signed-in landing page. For step 1 it shows who is signed in and proves that the deployment can
 * reach PostgreSQL and Azure Blob Storage. Later steps replace it with the assessment start page.
 */
@Route("")
@PageTitle("Home | TestMyLanguage")
@PermitAll
public class HomeView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public HomeView(SecurityService securityService,
                    LanguageContext languageContext,
                    JdbcTemplate jdbcTemplate,
                    AzureBlobService blobService,
                    @Value("${assessment.blob-check.container:}") String blobContainer,
                    @Value("${assessment.blob-check.blob:}") String blobName) {
        setAlignItems(Alignment.CENTER);
        getStyle().set("padding-top", "var(--lumo-space-xl)");

        UserDetails user = securityService.getAuthenticatedUser();
        String who = user != null ? user.getUsername() : "signed-in user";

        add(new H2("TestMyLanguage"));
        add(new Paragraph("Signed in as " + who));
        add(new Paragraph("Language: " + languageContext.getLanguage()));
        add(statusRow("PostgreSQL", checkDatabase(jdbcTemplate)));
        add(statusRow("Blob Storage", checkBlob(blobService, blobContainer, blobName)));

        add(new RouterLink("Start: welcome and activity introductions", WelcomeView.class));
        if (isAdmin()) {
            add(new RouterLink("Admin: review the proctor texts", ProctorReviewView.class));
            add(new RouterLink("Admin: handwriting check (Meta reads a drawing)", HandwritingCheckView.class));
        }

        Button logout = new Button("Sign out", e -> securityService.logout());
        add(logout);
    }

    private static boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static String checkDatabase(JdbcTemplate jdbcTemplate) {
        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            return Integer.valueOf(1).equals(one) ? "reachable" : "unexpected result";
        } catch (Exception e) {
            return "not reachable";
        }
    }

    private static String checkBlob(AzureBlobService blobService, String container, String blob) {
        if (!blobService.isConfigured()) {
            return "not configured";
        }
        if (container == null || container.isBlank() || blob == null || blob.isBlank()) {
            return "configured (no check blob set)";
        }
        return blobService.blobExists(container, blob) ? "readable" : "check blob not found";
    }

    private static Span statusRow(String label, String status) {
        return new Span(label + ": " + status);
    }
}
