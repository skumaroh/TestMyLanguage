package com.infotech.lang.application.views;

import com.infotech.lang.application.interest.InterestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Admin-only list of the "next level" interest answers. Only learners who ticked the contact box may be emailed about the next levels. */
@Route(value = "admin/interest", layout = MainLayout.class)
@PageTitle("Next-level interest | TestMyLanguage")
@RolesAllowed("ADMIN")
public class InterestAdminView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(InterestAdminView.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final transient InterestService interest;
    private final Grid<InterestService.Row> grid = new Grid<>(InterestService.Row.class, false);
    private final Div summary = new Div();

    public InterestAdminView(InterestService interest) {
        this.interest = interest;
        add(new H2("Next-level interest"), new Paragraph("Answers of learners who earned a certificate, newest first. Only email learners whose "
                + "'may contact' box is ticked, and only about the next levels and new modules. This list holds personal data: keep it private."),
                summary);
        grid.addColumn(r -> r.updated() == null ? "" : r.updated().format(DATE)).setHeader("When").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(InterestService.Row::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(InterestService.Row::email).setHeader("Email").setAutoWidth(true);
        grid.addColumn(InterestService.Row::language).setHeader("Language").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(InterestService.Row::interest).setHeader("Interest").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> r.contactOk() ? "Yes" : "No").setHeader("May contact").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(InterestService.Row::topics).setHeader("Topics").setAutoWidth(true);
        grid.addComponentColumn(r -> text(r.opinion())).setHeader("Opinion").setWidth("260px").setFlexGrow(1);
        grid.addComponentColumn(r -> text(r.suggestions())).setHeader("Suggestions").setWidth("260px").setFlexGrow(1);
        grid.setHeight("65vh");
        add(new Button("Refresh", e -> reload()), grid);
        setWidthFull();
        reload();
    }

    private void reload() {
        try {
            grid.setItems(interest.latest(500));
            int[] c = interest.counts();
            summary.setText("Very interested: " + c[0] + "   ·   Maybe: " + c[1] + "   ·   Not now: " + c[2] + "   ·   May contact: " + c[3]);
        } catch (RuntimeException e) {
            logger.error("Could not load the interest answers", e);
            Notification.show("The answers could not be loaded (has the V091 migration been run?).");
        }
    }

    private static Div text(String value) {
        Div d = new Div();
        d.setText(value == null ? "" : value);   // plain text, never HTML
        d.getStyle().set("white-space", "pre-wrap");
        return d;
    }
}
