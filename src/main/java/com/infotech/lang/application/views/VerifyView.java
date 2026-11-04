package com.infotech.lang.application.views;

import com.infotech.lang.application.certificate.CertificateAudit;
import com.infotech.lang.application.certificate.VerifyThrottle;
import com.infotech.lang.application.report.Certificate;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The public page where anyone (no sign-in) can check that a certificate is genuine by typing its verification code, or by scanning the QR
 * code on the certificate, which opens this page with the code filled in. It shows only the name, language, level and issue date. It never
 * shows an email address or the exact score.
 */
@Route(value = "verify")
@PageTitle("Verify a certificate | TestMyLanguage")
@AnonymousAllowed
public class VerifyView extends VerticalLayout implements BeforeEnterObserver {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(VerifyView.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final transient CertificateAudit audit;
    private final transient VerifyThrottle throttle;
    private final Div result = new Div();
    private final TextField code = new TextField("Verification code");

    public VerifyView(CertificateAudit audit, VerifyThrottle throttle) {
        this.audit = audit;
        this.throttle = throttle;
        setAlignItems(Alignment.CENTER);
        setMaxWidth("640px");
        getStyle().set("margin", "0 auto");
        add(new H1("Verify a certificate"), new Paragraph("Every TestMyLanguage certificate has a verification code printed on it, for example "
                + "K7QM-2XH9-WD4P. Type it here to check that the certificate is genuine."));
        code.setPlaceholder("XXXX-XXXX-XXXX");
        code.setMaxLength(20);
        code.setWidth("18em");
        Button check = new Button("Check", e -> check(code.getValue()));
        check.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(code, check, result);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        List<String> param = event.getLocation().getQueryParameters().getParameters().get("code");
        if (param != null && !param.isEmpty() && !param.get(0).isBlank()) {
            code.setValue(param.get(0));
            check(param.get(0));
        }
    }

    private void check(String typed) {
        result.removeAll();
        if (!throttle.allow(visitor(), System.currentTimeMillis())) {
            result.add(note("Too many checks in a short time. Please wait a few minutes and try again.", "var(--lumo-error-color)"));
            return;
        }
        String normalized = CertificateAudit.normalizeCode(typed);
        if (normalized == null) {
            result.add(note("That is not a valid code. A code has 12 letters and digits, like K7QM-2XH9-WD4P.", "var(--lumo-error-color)"));
            return;
        }
        try {
            Optional<CertificateAudit.Verified> found = audit.lookup(normalized);
            if (found.isEmpty()) {
                result.add(note("No certificate with this code was found. Please check the code and try again.", "var(--lumo-error-color)"));
                return;
            }
            CertificateAudit.Verified v = found.get();
            Div box = new Div();
            box.getStyle().set("border", "2px solid var(--lumo-success-color)").set("border-radius", "var(--lumo-border-radius-l)")
                    .set("padding", "var(--lumo-space-m)").set("background", "var(--lumo-success-color-10pct)");
            Paragraph genuine = new Paragraph("This certificate is genuine.");
            genuine.getStyle().set("font-weight", "700").set("font-size", "1.2em").set("margin-top", "0");
            box.add(genuine, new Paragraph("Awarded to: " + v.name()),
                    new Paragraph(Certificate.levelLabel(v.level()) + " placement assessment in "
                            + ("hindi".equalsIgnoreCase(v.language()) ? "Hindi" : "Tamil")),
                    new Paragraph("Issued on " + (v.issued() == null ? "" : v.issued().format(DATE)) + " (certificate " + v.number() + ")"));
            result.add(box);
        } catch (RuntimeException e) {
            logger.error("Could not look up a certificate", e);
            result.add(note("The check could not be made right now. Please try again later.", "var(--lumo-error-color)"));
        }
    }

    private static Paragraph note(String text, String color) {
        Paragraph p = new Paragraph(text);
        p.getStyle().set("color", color);
        return p;
    }

    /** The visitor for the throttle: the first address in X-Forwarded-For (set by the ingress), else the connection's address. */
    private static String visitor() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) {
            return "?";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
