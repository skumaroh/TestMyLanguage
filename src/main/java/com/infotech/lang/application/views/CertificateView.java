package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessAttempt;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.report.Certificate;
import com.infotech.lang.application.report.ReportService;
import com.infotech.lang.application.report.ReportSnapshot;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * A printable certificate for a learner who scored 90% or more in a full assessment. It has no menu so that the browser's Print (or Save as
 * PDF) gives a clean A4 landscape page. It is built from the stored report, so the numbers never change. The learner may correct the
 * name before printing; the corrected name is not stored. Only the learner (or an admin) can open it, and only when eligible.
 */
@NpmPackage(value = "jspdf", version = "2.5.2")
@NpmPackage(value = "html2canvas", version = "1.4.1")
@JsModule("./components/certificate-pdf.js")
@Route(value = "assessment/certificate")
@PageTitle("Certificate | TestMyLanguage")
@PermitAll
public class CertificateView extends VerticalLayout implements BeforeEnterObserver {

    private static final long serialVersionUID = 1L;

    private final transient ReportService reports;
    private final transient SecurityService securityService;
    private final transient UserRepository userRepository;

    public CertificateView(ReportService reports, SecurityService securityService, UserRepository userRepository) {
        this.reports = reports;
        this.securityService = securityService;
        this.userRepository = userRepository;
        setAlignItems(Alignment.CENTER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        try {
            UUID viewer = viewerId();
            List<String> param = event.getLocation().getQueryParameters().getParameters().get("attempt");
            Optional<AssessAttempt> attempt = viewer == null || param == null || param.isEmpty() ? Optional.empty()
                    : parse(param.get(0)).flatMap(reports::attempt).filter(a -> MainLayout.isAdmin() || viewer.equals(a.getUserId()));
            Optional<ReportSnapshot> report = attempt.flatMap(a -> reports.load(a.getId()));
            if (report.isEmpty()) {
                message("There is no certificate to show here.");
                return;
            }
            if (!Certificate.eligible(report.get())) {
                message("A certificate is given for a full assessment with a score of " + Math.round(Certificate.THRESHOLD_PERCENT)
                        + "% or more. This result does not reach that, so there is no certificate for it.");
                return;
            }
            show(report.get(), nameOf(attempt.get().getUserId()));
        } catch (RuntimeException e) {
            message("Sorry, the certificate could not be shown right now. Please try again in a moment.");
        }
    }

    private void show(ReportSnapshot r, String initialName) {
        // print rules: A4 landscape, and the buttons and the name box are not printed
        add(new Html("<style>@media print{.no-print{display:none !important} body{background:#fff !important}"
                + " @page{size:A4 landscape;margin:10mm}}</style>"));

        Span name = new Span(initialName);
        name.getStyle().set("font-size", "2.4em").set("font-weight", "700").set("border-bottom", "2px solid #444")
                .set("padding", "0 1em").set("display", "inline-block").set("min-width", "12em");

        TextField nameBox = new TextField("Name on the certificate (you can change it before printing; it is not saved)");
        nameBox.setValue(initialName);
        nameBox.setWidth("32em");
        nameBox.setMaxLength(80);
        nameBox.addValueChangeListener(e -> name.setText(e.getValue() == null || e.getValue().isBlank() ? "Learner" : e.getValue().trim()));
        nameBox.addClassName("no-print");

        Button print = new Button("Print");
        print.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        print.getElement().executeJs("this.addEventListener('click', () => window.print());");
        Button download = new Button("Download PDF");
        download.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button back = new Button("Back to my results", e -> getUI().ifPresent(ui -> ui.navigate("assessment/report",
                QueryParameters.simple(Map.of("attempt", String.valueOf(r.attemptId()))))));
        HorizontalLayout buttons = new HorizontalLayout(download, print, back);
        buttons.addClassName("no-print");
        add(nameBox, buttons);

        Div page = new Div();
        page.getStyle().set("border", "8px double #1f3b6f").set("padding", "2.5em 3em").set("max-width", "1000px").set("width", "100%")
                .set("box-sizing", "border-box").set("text-align", "center").set("background", "#fff").set("color", "#1b1b1b");
        H2 brand = new H2("TestMyLanguage");
        brand.getStyle().set("margin", "0").set("color", "#1f3b6f").set("letter-spacing", "0.08em");
        H1 title = new H1("Certificate of Achievement");
        title.getStyle().set("margin", "0.3em 0 0.6em 0").set("font-size", "2.6em");
        Paragraph awarded = line("This certificate is awarded to");
        Paragraph did = line("for completing the Level " + r.level() + " placement assessment in " + Certificate.languageName(r)
                + " with an overall score of " + Math.round(r.percent()) + "%.");
        did.getStyle().set("font-size", "1.3em");
        Paragraph when = line(Certificate.dateText(r).isEmpty() ? "" : "Completed on " + Certificate.dateText(r));
        Paragraph number = line("Certificate number " + Certificate.number(r));
        number.getStyle().set("font-size", "0.9em").set("color", "#555");
        Paragraph disclaimer = line(Certificate.DISCLAIMER);
        disclaimer.getStyle().set("font-size", "0.8em").set("color", "#555").set("margin-top", "1.5em");
        page.add(brand, title, awarded, name, did, when, number, disclaimer);
        add(page);
        // the PDF is made in the browser from the page as shown (so a Tamil or Hindi name is drawn correctly), including a corrected name
        download.getElement().executeJs("const page = $0, file = $1; this.addEventListener('click', () => "
                + "{ if (window.tmlCertificate) { window.tmlCertificate.download(page, file); } });",
                page.getElement(), "TestMyLanguage-certificate-" + Certificate.number(r) + ".pdf");
    }

    private static Paragraph line(String text) {
        Paragraph p = new Paragraph(text);
        p.getStyle().set("margin", "0.7em 0");
        return p;
    }

    private String nameOf(UUID userId) {
        return userId == null ? "Learner" : userRepository.findById(userId)
                .map(u -> Certificate.nameOf(u.getDisplayName(), u.getFirstName(), u.getLastName())).orElse("Learner");
    }

    private UUID viewerId() {
        UserDetails user = securityService.getAuthenticatedUser();
        return user == null ? null : userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }

    private static Optional<Long> parse(String text) {
        try {
            return Optional.of(Long.valueOf(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void message(String text) {
        add(new H2("Certificate"), new Paragraph(text), new Button("Back to my results", e -> getUI().ifPresent(ui -> ui.navigate("assessment/history"))));
    }
}
