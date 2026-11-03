package com.infotech.lang.application.views;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.feedback.Feedback;
import com.infotech.lang.application.feedback.FeedbackService;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Admin-only feedback report, newest first. Each row has editable Defect, Ticket (with an optional ticket reference) and Addressed
 * flags and a Save button. Feedback text is always shown as plain text.
 */
@Route(value = "admin/feedback", layout = MainLayout.class)
@PageTitle("Feedback report | TestMyLanguage")
@RolesAllowed("ADMIN")
public class FeedbackReportView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FeedbackReportView.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final transient FeedbackService service;
    private final transient SecurityService securityService;
    private final transient UserRepository userRepository;
    private final Grid<Feedback> grid = new Grid<>(Feedback.class, false);
    private final transient Map<UUID, String> emails = new HashMap<>();

    public FeedbackReportView(FeedbackService service, SecurityService securityService, UserRepository userRepository) {
        this.service = service;
        this.securityService = securityService;
        this.userRepository = userRepository;
        add(new H2("Feedback report"), new Paragraph("Newest first. Tick Defect, Ticket and Addressed as you triage, then press Save on that row."));

        grid.addColumn(f -> f.getCreatedAt().format(DATE)).setHeader("Sent").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(f -> email(f.getUserId())).setHeader("From").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(f -> "★".repeat(f.getStars()) + " (" + f.getStars() + ")").setHeader("Stars").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(f -> nz(f.getSourcePage())).setHeader("From page").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(f -> text(f.getExperience())).setHeader("Experience").setFlexGrow(1).setWidth("260px");
        grid.addComponentColumn(f -> text(f.getSuggestions())).setHeader("Suggestions / flaws").setFlexGrow(1).setWidth("260px");
        grid.addComponentColumn(this::triage).setHeader("Triage").setAutoWidth(true).setFlexGrow(0);
        grid.setAllRowsVisible(false);
        grid.setHeight("70vh");
        add(new Button("Refresh", e -> reload()), grid);
        setWidthFull();
        reload();
    }

    private void reload() {
        try {
            List<Feedback> rows = service.latest();
            emails.clear();
            userRepository.findAllById(rows.stream().map(Feedback::getUserId).filter(java.util.Objects::nonNull).distinct().toList())
                    .forEach(u -> emails.put(u.getId(), u.getEmail()));
            grid.setItems(rows);
        } catch (RuntimeException e) {
            logger.error("Could not load the feedback", e);
            Notification.show("The feedback could not be loaded (has the V089 migration been run?).");
        }
    }

    private HorizontalLayout triage(Feedback f) {
        Checkbox defect = new Checkbox("Defect", f.isDefect());
        Checkbox ticket = new Checkbox("Ticket", f.isTicket());
        TextField ref = new TextField();
        ref.setPlaceholder("Ticket number or link");
        ref.setMaxLength(300);
        ref.setValue(f.getTicketRef() == null ? "" : f.getTicketRef());
        ref.setWidth("180px");
        Checkbox addressed = new Checkbox("Addressed", f.isAddressed());
        Button save = new Button("Save", e -> {
            try {
                boolean ok = service.saveTriage(f.getId(), defect.getValue(), ticket.getValue(), ref.getValue(), addressed.getValue(), adminId());
                Notification.show(ok ? "Saved." : "That feedback no longer exists.");
            } catch (RuntimeException ex) {
                logger.error("Could not save the triage flags", ex);
                Notification.show("Could not save. Please try again.");
            }
        });
        HorizontalLayout row = new HorizontalLayout(defect, ticket, ref, addressed, save);
        row.setAlignItems(Alignment.CENTER);
        return row;
    }

    private static Div text(String value) {
        Div d = new Div();
        d.setText(value == null ? "" : value);   // plain text, never HTML
        d.getStyle().set("white-space", "pre-wrap").set("padding", "var(--lumo-space-xs) 0");
        return d;
    }

    private String email(UUID id) {
        return id == null ? "" : emails.getOrDefault(id, "unknown user");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private UUID adminId() {
        UserDetails user = securityService.getAuthenticatedUser();
        return user == null ? null : userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }
}
