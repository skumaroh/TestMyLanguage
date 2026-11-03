package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessAttempt;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.report.ReportService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin-only progress report of all students: every attempt (finished or still going), newest first, with the student's email, score and
 * result. A finished attempt opens its stored report (the report page lets admins open any student's attempt).
 */
@Route(value = "admin/progress", layout = MainLayout.class)
@PageTitle("Student progress | TestMyLanguage")
@RolesAllowed("ADMIN")
public class AdminProgressView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AdminProgressView.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final transient ReportService reports;
    private final transient UserRepository userRepository;
    private final Grid<AssessAttempt> grid = new Grid<>(AssessAttempt.class, false);
    private final Paragraph summary = new Paragraph();
    private final TextField search = new TextField("Student email contains");
    private final ComboBox<String> status = new ComboBox<>("Show");
    private final transient Map<UUID, String> emails = new HashMap<>();
    private transient List<AssessAttempt> all = List.of();

    public AdminProgressView(ReportService reports, UserRepository userRepository) {
        this.reports = reports;
        this.userRepository = userRepository;
        add(new H2("Student progress"), new Paragraph("Every student's assessment attempts, newest first (up to 500). "
                + "Open a finished attempt to see that student's stored report."));

        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> applyFilter());
        status.setItems("All attempts", "Finished", "Still in progress");
        status.setValue("All attempts");
        status.addValueChangeListener(e -> applyFilter());
        add(new HorizontalLayout(search, status, new Button("Refresh", e -> reload())), summary);

        grid.addColumn(a -> a.getCompletedAt() != null ? a.getCompletedAt().format(DATE)
                : a.getStartedAt() == null ? "" : a.getStartedAt().format(DATE)).setHeader("When").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(a -> email(a.getUserId())).setHeader("Student").setAutoWidth(true);
        grid.addColumn(a -> "hindi".equalsIgnoreCase(a.getLanguage()) ? "Hindi" : "Tamil").setHeader("Language").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(AssessAttempt::getLevel).setHeader("Level").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(a -> finished(a) ? "Finished" : "In progress").setHeader("Status").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(a -> a.getTotalAsked() == 0 ? "" : a.getTotalCorrect() + " of " + a.getTotalAsked()
                + (a.getPercent() == null ? "" : " (" + a.getPercent().setScale(0, java.math.RoundingMode.HALF_UP) + "%)"))
                .setHeader("Score").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(a -> !finished(a) ? "" : a.getLevelPassed() == null ? "Partial"
                : (a.getLevelPassed() ? "Level passed" : "Keep practising")).setHeader("Result").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(a -> {
            Button open = new Button("Open report", e -> getUI().ifPresent(ui -> ui.navigate("assessment/report",
                    QueryParameters.simple(Map.of("attempt", String.valueOf(a.getId()))))));
            open.setEnabled(finished(a));
            return open;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
        grid.setHeight("65vh");
        add(grid);
        setWidthFull();
        reload();
    }

    private void reload() {
        try {
            all = reports.allAttempts();
            emails.clear();
            userRepository.findAllById(all.stream().map(AssessAttempt::getUserId).filter(Objects::nonNull).distinct().toList())
                    .forEach(u -> emails.put(u.getId(), u.getEmail()));
        } catch (RuntimeException e) {
            logger.error("Could not load the student progress", e);
            all = List.of();
            Notification.show("The student progress could not be loaded right now.");
        }
        applyFilter();
    }

    private void applyFilter() {
        String q = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        String s = status.getValue();
        List<AssessAttempt> rows = all.stream()
                .filter(a -> q.isEmpty() || email(a.getUserId()).toLowerCase(Locale.ROOT).contains(q))
                .filter(a -> "Finished".equals(s) ? finished(a) : !"Still in progress".equals(s) || !finished(a))
                .toList();
        grid.setItems(rows);
        long students = rows.stream().map(AssessAttempt::getUserId).filter(Objects::nonNull).distinct().count();
        long done = rows.stream().filter(AdminProgressView::finished).count();
        long passed = rows.stream().filter(a -> finished(a) && Boolean.TRUE.equals(a.getLevelPassed())).count();
        summary.setText(rows.size() + " attempt(s) by " + students + " student(s); " + done + " finished, " + passed + " passed the level.");
    }

    private static boolean finished(AssessAttempt a) {
        return "COMPLETED".equals(a.getStatus());
    }

    private String email(UUID id) {
        return id == null ? "" : emails.getOrDefault(id, "unknown user");
    }
}
