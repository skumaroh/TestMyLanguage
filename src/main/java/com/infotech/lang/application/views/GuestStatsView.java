package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.Modules;
import com.infotech.lang.application.guest.GuestService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.Map;

/** Admin-only: how many guest sessions there were, per day, and how far they got. Anonymous: no person can be identified from it. */
@Route(value = "admin/guest-stats", layout = MainLayout.class)
@PageTitle("Guest statistics | TestMyLanguage")
@RolesAllowed("ADMIN")
public class GuestStatsView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public GuestStatsView(GuestService guests) {
        setMaxWidth("980px");
        getStyle().set("margin", "0 auto");
        add(new H2("Guest statistics"));
        add(new Paragraph("Guest mode is " + (guests.enabled() ? "on" : "off") + ". These counts are of guest sessions, not of people "
                + "(a visitor can start several), and hold no identity, address or answers. Last 30 days, in UTC."));
        List<GuestService.DayRow> rows = guests.statsByDay(30);
        long started = rows.stream().mapToLong(GuestService.DayRow::started).sum();
        long finished = rows.stream().mapToLong(GuestService.DayRow::finished).sum();
        long google = rows.stream().mapToLong(GuestService.DayRow::google).sum();
        long register = rows.stream().mapToLong(GuestService.DayRow::register).sum();
        add(new Paragraph("Started: " + started + " · Reached the summary: " + finished + " · Clicked Google sign-in: " + google
                + " · Clicked create an account: " + register));
        Map<String, Long> modules = guests.modulesDone(30);
        StringBuilder done = new StringBuilder("Modules finished by guests: ");
        for (Modules m : Modules.values()) {
            done.append(m.number()).append(" = ").append(modules.getOrDefault(m.code(), 0L)).append("   ");
        }
        add(new Paragraph(done.toString()));
        Grid<GuestService.DayRow> grid = new Grid<>(GuestService.DayRow.class, false);
        grid.addColumn(GuestService.DayRow::day).setHeader("Day").setAutoWidth(true);
        grid.addColumn(GuestService.DayRow::language).setHeader("Language").setAutoWidth(true);
        grid.addColumn(GuestService.DayRow::started).setHeader("Started").setAutoWidth(true);
        grid.addColumn(GuestService.DayRow::moduleDone).setHeader("Modules finished").setAutoWidth(true);
        grid.addColumn(GuestService.DayRow::finished).setHeader("Reached summary").setAutoWidth(true);
        grid.addColumn(GuestService.DayRow::google).setHeader("Google clicks").setAutoWidth(true);
        grid.addColumn(GuestService.DayRow::register).setHeader("Register clicks").setAutoWidth(true);
        grid.setItems(rows);
        grid.setAllRowsVisible(true);
        add(grid);
    }
}
