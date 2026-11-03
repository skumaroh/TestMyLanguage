package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessAttempt;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.report.ReportService;
import com.infotech.lang.application.report.ReportSnapshot;
import com.infotech.lang.application.security.SecurityService;
import com.infotech.lang.application.voice.SpeechRegistry;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The final report of a completed assessment: the level result, the overall score, points and pass or not per block, a short written
 * summary from Meta, and every question with the student's answer, the expected answer and notes (spoken, help used, spelling).
 * {@code ?attempt=<id>} opens a specific attempt (only your own, or any for an admin); without it, your latest completed attempt.
 * The report is stored when the attempt is completed, so it never changes afterwards.
 */
@Route(value = "assessment/report", layout = MainLayout.class)
@PageTitle("Your results | TestMyLanguage")
@PermitAll
public class ReportView extends VerticalLayout implements BeforeEnterObserver {

    private static final long serialVersionUID = 1L;

    private final transient ReportService reports;
    private final transient SecurityService securityService;
    private final transient UserRepository userRepository;
    private final transient SpeechRegistry speechRegistry;

    public ReportView(ReportService reports, SecurityService securityService, UserRepository userRepository,
                      SpeechRegistry speechRegistry) {
        this.reports = reports;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.speechRegistry = speechRegistry;
        setAlignItems(Alignment.CENTER);
        setMaxWidth("860px");
        getStyle().set("margin", "0 auto");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        try {
            UUID userId = userIdOf();
            if (userId == null) {
                message("Please sign in again to see your results.");
                return;
            }
            List<String> param = event.getLocation().getQueryParameters().getParameters().get("attempt");
            Optional<AssessAttempt> attempt;
            if (param != null && !param.isEmpty()) {
                attempt = parse(param.get(0)).flatMap(reports::attempt)
                        .filter(a -> MainLayout.isAdmin() || userId.equals(a.getUserId()));
            } else {
                attempt = reports.history(userId).stream().findFirst();
            }
            Optional<ReportSnapshot> report = attempt.flatMap(a -> reports.load(a.getId()));
            if (report.isEmpty()) {
                message("There are no results to show yet. Finish an assessment to see them here.");
                return;
            }
            show(report.get());
        } catch (RuntimeException e) {
            message("Sorry, the results could not be shown right now. Please try again in a moment.");
        }
    }

    private void show(ReportSnapshot r) {
        String language = "hindi".equalsIgnoreCase(r.language()) ? "Hindi" : "Tamil";
        add(new H2("Your results – " + language + ", Level " + r.level()));
        Div result = new Div(new Span(r.levelMessage()));
        result.getStyle().set("font-size", "1.3em").set("font-weight", "600").set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-l)").set("width", "100%").set("box-sizing", "border-box")
                .set("background", Boolean.TRUE.equals(r.levelPassed()) ? "var(--lumo-success-color-10pct)" : "var(--lumo-contrast-10pct)");
        add(result);
        add(new Paragraph("Overall: " + r.totalCorrect() + " of " + r.totalAsked() + " correct (" + Math.round(r.percent())
                + "%). The pass mark for the level is 75% overall."));

        add(new H3("Your results by part"));
        for (ReportSnapshot.Block b : r.blocks()) {
            Div row = new Div();
            row.getStyle().set("display", "flex").set("justify-content", "space-between").set("width", "100%")
                    .set("padding", "var(--lumo-space-xs) 0").set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
            Span name = new Span(b.title());
            Span score = new Span(b.correct() + " of " + b.asked() + " – " + (Boolean.TRUE.equals(b.passed()) ? "passed" : "keep practising"));
            score.getStyle().set("color", Boolean.TRUE.equals(b.passed()) ? "var(--lumo-success-text-color)" : "var(--lumo-secondary-text-color)");
            row.add(name, score);
            add(row);
        }

        if (r.summary() != null && !r.summary().isBlank()) {
            add(new H3("A few words for you"));
            Div summary = new Div();
            for (String paragraph : r.summary().split("\n+")) {
                if (!paragraph.isBlank()) {
                    summary.add(new Paragraph(paragraph.trim()));
                }
            }
            summary.setWidthFull();
            Span source = new Span("Written by Meta Muse Spark from your results. It cannot change your score.");
            source.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
            add(summary, new SpeakButton(r.summary().replaceAll("\\s+", " "), "english", speechRegistry), source);
        }

        add(new H3("Every question"));
        add(new Paragraph("A tick means the answer counted. Open a part to see your answers, the expected answers and notes."));
        Accordion accordion = new Accordion();
        accordion.setWidthFull();
        for (ReportSnapshot.Block b : r.blocks()) {
            VerticalLayout panel = new VerticalLayout();
            panel.setPadding(false);
            for (ReportSnapshot.Question q : r.questions()) {
                if (q.blockCode().equals(b.code())) {
                    panel.add(questionView(q));
                }
            }
            accordion.add(b.title() + " – " + b.correct() + " of " + b.asked(), panel);
        }
        add(accordion);
        if (com.infotech.lang.application.report.Certificate.eligible(r)) {
            Button certificate = new Button("Print your certificate", e -> getUI().ifPresent(ui -> ui.navigate("assessment/certificate",
                    com.vaadin.flow.router.QueryParameters.simple(java.util.Map.of("attempt", String.valueOf(r.attemptId()))))));
            certificate.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
            add(certificate);
        }
        add(new Button("See all my results", e -> getUI().ifPresent(ui -> ui.navigate("assessment/history"))));
        add(new Button("Give feedback", e -> getUI().ifPresent(ui -> ui.navigate("feedback/results"))));
    }

    private Div questionView(ReportSnapshot.Question q) {
        Div d = new Div();
        d.getStyle().set("padding", "var(--lumo-space-s)").set("border-left", "4px solid "
                + (q.correct() ? "var(--lumo-success-color)" : "var(--lumo-contrast-30pct)")).set("margin-bottom", "var(--lumo-space-s)");
        Span head = new Span((q.correct() ? "✓ " : "✗ ") + q.position() + ". " + (q.asked() == null || q.asked().isBlank() ? "" : q.asked()));
        head.getStyle().set("font-weight", "600");
        d.add(head);
        d.add(line("Your answer: ", q.yourAnswer()));
        if (q.expected() != null && !q.expected().isBlank()) {
            d.add(line("Expected: ", q.expected()));
        }
        for (String note : q.notes()) {
            Paragraph p = new Paragraph(note);
            p.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
            d.add(p);
        }
        return d;
    }

    private static Paragraph line(String label, String value) {
        Paragraph p = new Paragraph(label + (value == null ? "" : value));
        p.getStyle().set("margin", "0");
        return p;
    }

    private void message(String text) {
        add(new H2("Your results"), new Paragraph(text));
    }

    private static Optional<Long> parse(String text) {
        try {
            return Optional.of(Long.valueOf(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private UUID userIdOf() {
        UserDetails user = securityService.getAuthenticatedUser();
        return user == null ? null : userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }
}
