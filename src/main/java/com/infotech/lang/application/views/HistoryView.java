package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessAttempt;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.report.ReportService;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

/** A student's completed assessments: date, level, overall score and result. Each opens its stored report. */
@Route(value = "assessment/history", layout = MainLayout.class)
@PageTitle("My results | TestMyLanguage")
@PermitAll
public class HistoryView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public HistoryView(ReportService reports, SecurityService securityService, UserRepository userRepository) {
        add(new H2("My results"));
        UserDetails user = securityService.getAuthenticatedUser();
        UUID userId = user == null ? null : userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
        if (userId == null) {
            add(new Paragraph("Please sign in again."));
            return;
        }
        List<AssessAttempt> attempts;
        try {
            attempts = reports.history(userId);
        } catch (RuntimeException e) {
            add(new Paragraph("Your results could not be loaded right now."));
            return;
        }
        if (attempts.isEmpty()) {
            add(new Paragraph("You have not finished an assessment yet."));
            return;
        }
        Grid<AssessAttempt> grid = new Grid<>(AssessAttempt.class, false);
        DateTimeFormatter date = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");
        grid.addColumn(a -> a.getCompletedAt() == null ? "" : a.getCompletedAt().format(date)).setHeader("Finished").setAutoWidth(true);
        grid.addColumn(a -> "hindi".equalsIgnoreCase(a.getLanguage()) ? "Hindi" : "Tamil").setHeader("Language").setAutoWidth(true);
        grid.addColumn(AssessAttempt::getLevel).setHeader("Level").setAutoWidth(true);
        grid.addColumn(a -> a.getTotalCorrect() + " of " + a.getTotalAsked() + (a.getPercent() == null ? ""
                : " (" + a.getPercent().setScale(0, java.math.RoundingMode.HALF_UP) + "%)")).setHeader("Score").setAutoWidth(true);
        grid.addColumn(a -> a.getLevelPassed() == null ? "Partial" : (a.getLevelPassed() ? "Level passed" : "Keep practising"))
                .setHeader("Result").setAutoWidth(true);
        grid.addComponentColumn(a -> new Button("Open", e -> getUI().ifPresent(ui ->
                ui.navigate("assessment/report", com.vaadin.flow.router.QueryParameters.simple(
                        java.util.Map.of("attempt", String.valueOf(a.getId()))))))).setHeader("");
        grid.setItems(attempts);
        grid.setAllRowsVisible(true);
        add(grid);
    }
}
