package com.infotech.lang.application.views;

import com.infotech.lang.application.certificate.CertificateAudit;
import com.infotech.lang.application.certificate.CertificateNameService;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Admin-only certificate log: every certificate issued, every Download or Print click, and every admin reset of a certificate name, with the
 * learner's email and the name as printed. Also the one place a certificate name can be reset (so a learner can enter it again); each reset
 * is written to the log. The log itself cannot be edited from the application.
 */
@Route(value = "admin/certificates", layout = MainLayout.class)
@PageTitle("Certificates | TestMyLanguage")
@RolesAllowed("ADMIN")
public class CertificateAdminView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CertificateAdminView.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final transient CertificateAudit audit;
    private final transient CertificateNameService names;
    private final transient SecurityService securityService;
    private final transient UserRepository userRepository;
    private final Grid<CertificateAudit.Row> grid = new Grid<>(CertificateAudit.Row.class, false);
    private final TextField search = new TextField("Email contains");
    private final ComboBox<String> action = new ComboBox<>("Show");

    public CertificateAdminView(CertificateAudit audit, CertificateNameService names, SecurityService securityService,
                                UserRepository userRepository) {
        this.audit = audit;
        this.names = names;
        this.securityService = securityService;
        this.userRepository = userRepository;
        add(new H2("Certificates"), new Paragraph("Newest first (up to 500). ISSUED is the first time a certificate was shown, DOWNLOAD and "
                + "PRINT are button clicks (a browser cannot prove that paper was printed), NAME_RESET is an administrator clearing a name. "
                + "The log cannot be edited from the application. It holds personal data (name and email): keep it private."));

        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> reload());
        action.setItems("All", CertificateAudit.ISSUED, CertificateAudit.DOWNLOAD, CertificateAudit.PRINT, CertificateAudit.NAME_RESET);
        action.setValue("All");
        action.addValueChangeListener(e -> reload());
        add(new HorizontalLayout(search, action, new Button("Refresh", e -> reload())));

        grid.addColumn(r -> r.when() == null ? "" : r.when().format(DATE)).setHeader("When").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(CertificateAudit.Row::action).setHeader("Action").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(CertificateAudit.Row::email).setHeader("Email").setAutoWidth(true);
        grid.addColumn(CertificateAudit.Row::name).setHeader("Name on certificate").setAutoWidth(true);
        grid.addColumn(CertificateAudit.Row::number).setHeader("Number").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(CertificateAudit.Row::code).setHeader("Verification code").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> r.language() == null ? "" : r.language() + (r.level() == null ? "" : " L" + r.level()))
                .setHeader("Language").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> r.percent() == null ? "" : r.percent().stripTrailingZeros().toPlainString() + "%").setHeader("Score")
                .setAutoWidth(true).setFlexGrow(0);
        grid.setHeight("55vh");

        TextField resetEmail = new TextField("Reset the certificate name of (email)");
        resetEmail.setWidth("24em");
        Button reset = new Button("Reset name", e -> confirmReset(resetEmail.getValue()));
        HorizontalLayout resetRow = new HorizontalLayout(resetEmail, reset);
        resetRow.setAlignItems(Alignment.END);
        add(grid, new H3("Fix a mistake"), new Paragraph("A learner cannot change their certificate name. If it has a typo, reset it here; "
                + "they can then enter it again. The reset is written to the log."), resetRow);
        setWidthFull();
        reload();
    }

    private void reload() {
        try {
            List<CertificateAudit.Row> rows = audit.latest(500, search.getValue(), "All".equals(action.getValue()) ? null : action.getValue());
            grid.setItems(rows);
        } catch (RuntimeException e) {
            logger.error("Could not load the certificate log", e);
            Notification.show("The certificate log could not be loaded (has the V090 migration been run?).");
        }
    }

    private void confirmReset(String email) {
        Optional<User> user = email == null || email.isBlank() ? Optional.empty() : userRepository.findByEmailIgnoreCase(email.trim());
        if (user.isEmpty()) {
            Notification.show("No user with that email.");
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Reset certificate name");
        dialog.setText("Clear the certificate name of " + user.get().getEmail() + "? They will be able to enter it again.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Reset");
        dialog.addConfirmListener(e -> {
            try {
                String old = names.get(user.get().getId()).orElse(null);
                if (old == null || !names.clear(user.get().getId())) {
                    Notification.show("That user has no certificate name to reset.");
                    return;
                }
                audit.nameReset(user.get().getId(), adminId(), user.get().getEmail(), old);
                Notification.show("Reset. The learner can enter the name again.");
                reload();
            } catch (RuntimeException ex) {
                logger.error("Could not reset the certificate name", ex);
                Notification.show("Could not reset the name. Please try again.");
            }
        });
        dialog.open();
    }

    private UUID adminId() {
        UserDetails details = securityService.getAuthenticatedUser();
        return details == null ? null : userRepository.findByEmailIgnoreCase(details.getUsername()).map(User::getId).orElse(null);
    }
}
