package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.ModuleProgressService;
import com.infotech.lang.application.assessment.Modules;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.proctor.ProctorTextService;
import com.infotech.lang.application.proctor.ProctorWarmUp;
import com.infotech.lang.application.report.ReportService;
import com.infotech.lang.application.video.VideoPicker;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.QueryParameters;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
 * Signed-in landing page: a welcome (back) line, the start / continue link with the Help popup, and the six modules with the ones
 * finished ticked. Admins also see the database and Blob Storage checks and the admin links.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home | TestMyLanguage")
@PermitAll
public class HomeView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public HomeView(SecurityService securityService,
                    LanguageContext languageContext,
                    JdbcTemplate jdbcTemplate,
                    AzureBlobService blobService,
                    UserRepository userRepository,
                    ModuleProgressService moduleProgress,
                    VideoPicker videoPicker,
                    ReportService reportService,
                    ProctorTextService proctorTexts,
                    ProctorWarmUp proctorWarmUp,
                    @Value("${proctor.level:1}") int level,
                    @Value("${assessment.blob-check.container:}") String blobContainer,
                    @Value("${assessment.blob-check.blob:}") String blobName) {
        setAlignItems(Alignment.CENTER);
        getStyle().set("padding-top", "var(--lumo-space-xl)");
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");

        UserDetails user = securityService.getAuthenticatedUser();
        Optional<User> student = user == null ? Optional.empty() : userRepository.findByEmailIgnoreCase(user.getUsername());
        String name = student.map(HomeView::preferredName).orElse("");
        UUID userId = student.map(User::getId).orElse(null);

        add(new H2("Test my " + languageContext.getDisplayName() + " language skills"));

        ModuleProgressService.Progress progress = userId == null ? new ModuleProgressService.Progress(null, Set.of(), false)
                : moduleProgress.of(userId);
        boolean returning = progress.hasCompletedBefore() || !progress.finished().isEmpty();
        add(new Paragraph(greeting(name, returning)));

        boolean videoAvailable = false;
        try {
            videoAvailable = videoPicker.isAvailable(languageContext.getLanguage(), 1);
        } catch (RuntimeException e) {
            // no video clips counted: the assessment simply has five modules
        }
        Optional<Modules> next = Modules.firstUnfinished(progress.finished(), videoAvailable);

        // Start / continue / take it again, as a link, with the welcome texts under a Help link beside it
        HorizontalLayout start = new HorizontalLayout();
        start.setAlignItems(Alignment.CENTER);
        if (next.isPresent()) {
            String label = !progress.finished().isEmpty() ? "Continue assessment"
                    : progress.hasCompletedBefore() ? "Take it again" : "Start assessment";
            start.add(link(label, next.get().route()));
        } else if (progress.attemptId() != null) {
            Long attemptId = progress.attemptId();
            Button results = new Button("See your results", e -> {
                reportService.complete(attemptId);
                getUI().ifPresent(ui -> ui.navigate("assessment/report", QueryParameters.simple(Map.of("attempt", String.valueOf(attemptId)))));
            });
            results.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            start.add(results);
        }
        start.add(HelpButton.link("Welcome and activity introductions",
                () -> new WelcomeIntro(securityService, userRepository, proctorTexts, proctorWarmUp, level)));
        add(start);

        add(new H3("Modules"));
        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        for (Modules module : Modules.taken(videoAvailable)) {
            boolean done = progress.finished().contains(module.code());
            if (done) {
                Span finished = new Span("✓ " + module.heading());
                finished.getStyle().set("color", "var(--lumo-success-text-color)");
                list.add(finished);
            } else if (next.isPresent() && next.get() == module) {
                list.add(link(module.heading(), module.route()));
            } else {
                Span later = new Span(module.heading());
                later.getStyle().set("color", "var(--lumo-disabled-text-color)");
                list.add(later);
            }
        }
        add(list);
        if (progress.hasCompletedBefore()) {
            add(new RouterLink("My results", HistoryView.class));
        }

        if (isAdmin()) {
            add(new H3("Admin"));
            add(statusRow("PostgreSQL", checkDatabase(jdbcTemplate)));
            add(statusRow("Blob Storage", checkBlob(blobService, blobContainer, blobName)));
            add(new RouterLink("Test Blocks", AdminBlocksView.class));
            add(new RouterLink("Admin: review the proctor texts", ProctorReviewView.class));
            add(new RouterLink("Admin: handwriting check (shape comparison)", HandwritingCheckView.class));
            add(new RouterLink("Admin: translation check (Meta grades meaning)", TranslationCheckView.class));
        }

        Button logout = new Button("Sign out", e -> securityService.logout());
        add(logout);
    }

    /** "Welcome back, Priya" for someone who has been here before; plain "Welcome" otherwise (and without a name). */
    static String greeting(String name, boolean returning) {
        String who = name == null || name.isBlank() ? "" : ", " + name.trim();
        return (returning ? "Welcome back" : "Welcome") + who + ". "
                + (returning ? "Pick up where you left off, or look at your results." : "Take the modules one by one; each takes a few minutes.");
    }

    private static Anchor link(String text, String route) {
        Anchor a = new Anchor(route, text);
        a.getElement().setAttribute("router-link", true);
        return a;
    }

    /** The name the student chose to be called (display name), then the first name. */
    private static String preferredName(User user) {
        String display = user.getDisplayName();
        if (display != null && !display.isBlank()) {
            return display.trim();
        }
        String first = user.getFirstName();
        return first == null ? "" : first.trim();
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
