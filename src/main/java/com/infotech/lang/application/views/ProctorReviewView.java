package com.infotech.lang.application.views;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.proctor.ProctorSlot;
import com.infotech.lang.application.proctor.ProctorStatus;
import com.infotech.lang.application.proctor.ProctorText;
import com.infotech.lang.application.proctor.ProctorTextService;
import com.infotech.lang.application.proctor.ProctorWarmUp;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Admin-only review of the proctor texts. A newly written text is a PENDING candidate that students never see.
 * An admin reads it and chooses Approve, Edit and approve, or Discard.
 */
@Route(value = "admin/proctor-texts", layout = MainLayout.class)
@PageTitle("Proctor texts | TestMyLanguage")
@RolesAllowed("ADMIN")
public class ProctorReviewView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final ProctorTextService service;
    private final ProctorWarmUp warmUp;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final int level;

    private final Grid<ProctorText> grid = new Grid<>(ProctorText.class, false);
    private final TextArea preview = new TextArea("Text (English, then the phrase in the tested language)");
    private final Button approve = new Button("Approve");
    private final Button edit = new Button("Edit and approve");
    private final Button discard = new Button("Discard");
    private final Button generateOne = new Button("Write a new candidate for this slot");

    public ProctorReviewView(ProctorTextService service,
                             ProctorWarmUp warmUp,
                             SecurityService securityService,
                             UserRepository userRepository,
                             @Value("${proctor.level:1}") int level) {
        this.service = service;
        this.warmUp = warmUp;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.level = level;

        add(new H2("Proctor texts"));
        add(new Paragraph("Texts are written by Meta Muse Spark and stay hidden from students until you approve them. "
                + "Discarded texts are kept for the record. Until a text is approved, students see a plain built-in text."));

        grid.addColumn(ProctorText::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(t -> t.getPurpose().name()).setHeader("Purpose").setAutoWidth(true);
        grid.addColumn(t -> t.getActivity().name()).setHeader("Activity").setAutoWidth(true);
        grid.addColumn(ProctorText::getLevel).setHeader("Level").setAutoWidth(true);
        grid.addColumn(ProctorText::getProctorLanguage).setHeader("Language").setAutoWidth(true);
        grid.addColumn(t -> t.getStatus().name()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(ProctorText::getSource).setHeader("Source").setAutoWidth(true);
        grid.addColumn(t -> t.getCreatedAt().toLocalDate().toString()).setHeader("Created").setAutoWidth(true);
        grid.setHeight("320px");
        grid.asSingleSelect().addValueChangeListener(e -> showSelection(e.getValue()));

        preview.setReadOnly(true);
        preview.setWidthFull();
        preview.setMinHeight("120px");

        approve.addClickListener(e -> run(() -> service.approve(selectedId(), reviewerId()), "Approved."));
        discard.addClickListener(e -> run(() -> service.discard(selectedId(), reviewerId()), "Discarded."));
        edit.addClickListener(e -> openEditor());
        generateOne.addClickListener(e -> generateForSelection());
        Button generateMissing = new Button("Write candidates for every empty slot", e -> {
            ProctorSlot.allForLevel(level, service.proctorLanguage()).forEach(warmUp::requestGenerationIfNeeded);
            Notification.show("Started in the background. Refresh in a minute or two.");
        });
        Button refresh = new Button("Refresh", e -> reload());

        add(grid, preview, new HorizontalLayout(approve, edit, discard, generateOne), new HorizontalLayout(generateMissing, refresh));
        setWidthFull();
        reload();
    }

    private void reload() {
        try {
            grid.setItems(service.listAll());
        } catch (RuntimeException e) {
            Notification.show("The proctor text tables are not available yet (has the migration been run?).");
        }
        showSelection(null);
    }

    private void showSelection(ProctorText text) {
        boolean pending = text != null && text.getStatus() == ProctorStatus.PENDING;
        preview.setValue(text == null ? "" : text.getBody());
        approve.setEnabled(pending);
        edit.setEnabled(pending);
        discard.setEnabled(text != null && text.getStatus() != ProctorStatus.DISCARDED);
        generateOne.setEnabled(text != null);
    }

    private long selectedId() {
        ProctorText selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            throw new IllegalStateException("select a text first");
        }
        return selected.getId();
    }

    private UUID reviewerId() {
        UserDetails user = securityService.getAuthenticatedUser();
        if (user == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }

    private void run(Runnable action, String done) {
        try {
            action.run();
            Notification.show(done);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Notification.show(e.getMessage());
        }
        reload();
    }

    private void openEditor() {
        ProctorText selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit and approve");
        TextArea editor = new TextArea("Text");
        editor.setValue(selected.getBody());
        editor.setWidth("560px");
        editor.setMinHeight("160px");
        Button save = new Button("Save and approve", e -> {
            run(() -> service.editAndApprove(selected.getId(), editor.getValue(), reviewerId()), "Edited and approved.");
            dialog.close();
        });
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.add(new VerticalLayout(
                new Paragraph("First line: English. Second line: the short phrase in the tested language. "
                        + "{name} is replaced by the student's first name."),
                editor));
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void generateForSelection() {
        ProctorText selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            return;
        }
        // Takes a few seconds: it waits for the model.
        ProctorTextService.GenerationOutcome outcome = service.generateCandidate(selected.slot(), true);
        Notification.show(switch (outcome) {
            case CREATED -> "A new candidate was written.";
            case SKIPPED -> "There is already a candidate waiting for review for this slot.";
            case REJECTED -> "The model's text failed the checks. Try again.";
            case FAILED -> "The model could not be reached.";
        });
        reload();
    }
}
