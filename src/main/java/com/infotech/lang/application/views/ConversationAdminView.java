package com.infotech.lang.application.views;

import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.conversation.Conversation;
import com.infotech.lang.application.conversation.ConversationLibrary;
import com.infotech.lang.application.conversation.ConversationObjective;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Admin-only conversation library. The model writes imagined conversations as DRAFTs in the background; an admin reads each one
 * (the persona's lines in the tested language, the objectives), edits it if needed, and verifies it. Only verified conversations
 * are used in a test. Retired conversations are kept for the record.
 */
@Route(value = "admin/conversations", layout = MainLayout.class)
@PageTitle("Conversations | TestMyLanguage")
@RolesAllowed("ADMIN")
public class ConversationAdminView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final int LEVEL = 1;

    private final ConversationLibrary library;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final String language;

    private final Grid<Conversation> grid = new Grid<>(Conversation.class, false);
    private final TextArea preview = new TextArea("Selected conversation");
    private final Button verify = new Button("Verify");
    private final Button edit = new Button("Edit");
    private final Button retire = new Button("Retire");

    public ConversationAdminView(ConversationLibrary library, SecurityService securityService, UserRepository userRepository,
                                 LanguageContext languageContext) {
        this.library = library;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.language = languageContext.getLanguage();

        add(new H2("Conversations (" + language + ")"));
        add(new Paragraph("Meta writes imagined everyday conversations as drafts. Read each one, fix anything odd in the "
                + language + " lines, and verify it. Only verified conversations are used in a test; a learner never gets "
                + "the same conversation twice. Retired conversations are kept but never used."));

        grid.addColumn(Conversation::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Conversation::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(Conversation::getScenarioTitle).setHeader("Scenario").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Conversation::getPersonaName).setHeader("Persona").setAutoWidth(true);
        grid.addColumn(Conversation::getLevel).setHeader("Level").setAutoWidth(true);
        grid.addColumn(Conversation::getDifficulty).setHeader("Difficulty").setAutoWidth(true);
        grid.addColumn(Conversation::getSource).setHeader("Source").setAutoWidth(true);
        grid.addColumn(Conversation::getTimesUsed).setHeader("Used").setAutoWidth(true);
        grid.setHeight("320px");
        grid.asSingleSelect().addValueChangeListener(e -> showSelection(e.getValue()));

        preview.setReadOnly(true);
        preview.setWidthFull();
        preview.setMinHeight("260px");

        IntegerField count = new IntegerField("How many to write");
        count.setValue(3);
        count.setMin(1);
        count.setMax(5);
        count.setStepButtonsVisible(true);
        Button generate = new Button("Write new drafts with Meta", e -> {
            int n = library.generateInBackground(language, LEVEL, count.getValue() == null ? 1 : count.getValue());
            Notification.show("Writing " + n + " draft(s) in the background. Press Refresh in a minute or two.");
        });
        Button refresh = new Button("Refresh", e -> reload());

        verify.addClickListener(e -> run(() -> library.verify(selectedId(), adminId()), "Verified."));
        retire.addClickListener(e -> run(() -> library.retire(selectedId()), "Retired."));
        edit.addClickListener(e -> openEditor());

        add(new HorizontalLayout(count, generate, refresh));
        add(grid, preview, new HorizontalLayout(verify, edit, retire));
        setWidthFull();
        reload();
    }

    private void reload() {
        try {
            grid.setItems(library.list(language));
        } catch (RuntimeException e) {
            Notification.show("The conversation tables are not available yet (has the V086 migration been run?).");
        }
        showSelection(null);
    }

    private void showSelection(Conversation c) {
        verify.setEnabled(c != null && !Conversation.VERIFIED.equals(c.getStatus()) && !Conversation.RETIRED.equals(c.getStatus()));
        edit.setEnabled(c != null && !Conversation.RETIRED.equals(c.getStatus()));
        retire.setEnabled(c != null && !Conversation.RETIRED.equals(c.getStatus()));
        if (c == null) {
            preview.setValue("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(c.getScenarioTitle()).append("  [").append(c.getStatus()).append(", ").append(c.getDifficulty()).append("]\n");
        sb.append("Persona: ").append(c.getPersonaName()).append(" – ").append(nullToEmpty(c.getPersonaDescription())).append("\n");
        sb.append("Note to the learner: ").append(nullToEmpty(c.getSceneNote())).append("\n\n");
        sb.append("OPENING: ").append(c.getIntroLine()).append("\n   (").append(nullToEmpty(c.getIntroEnglish())).append(")\n\n");
        for (ConversationObjective o : library.objectivesOf(c.getId())) {
            sb.append(o.getPosition()).append(". ").append(o.getObjectiveEn()).append("\n   hint: ")
                    .append(nullToEmpty(o.getVocabularyHint())).append("\n   persona asks: ").append(o.getFallbackLine()).append("\n");
        }
        sb.append("\nCLOSING: ").append(c.getClosingLine()).append("\n   (").append(nullToEmpty(c.getClosingEnglish())).append(")");
        preview.setValue(sb.toString());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private long selectedId() {
        Conversation selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            throw new IllegalStateException("Select a conversation first.");
        }
        return selected.getId();
    }

    private UUID adminId() {
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
        Conversation c = grid.asSingleSelect().getValue();
        if (c == null) {
            return;
        }
        List<ConversationObjective> objectives = library.objectivesOf(c.getId());
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit conversation " + c.getId() + (Conversation.VERIFIED.equals(c.getStatus())
                ? " (it goes back to draft until verified again)" : ""));
        dialog.setWidth("760px");

        TextField title = field("Scenario (English)", c.getScenarioTitle());
        TextField persona = field("Persona name (" + language + ")", c.getPersonaName());
        TextArea description = area("Who the persona is (English)", c.getPersonaDescription(), "70px");
        TextArea scene = area("Note shown to the learner (English)", c.getSceneNote(), "70px");
        TextArea intro = area("Opening line (" + language + ")", c.getIntroLine(), "70px");
        TextArea introEn = area("Opening line (English)", c.getIntroEnglish(), "60px");
        TextArea closing = area("Closing line (" + language + ")", c.getClosingLine(), "70px");
        TextArea closingEn = area("Closing line (English)", c.getClosingEnglish(), "60px");
        VerticalLayout form = new VerticalLayout(title, persona, description, scene, intro, introEn, closing, closingEn);
        form.setPadding(false);

        List<TextField> objectiveFields = new ArrayList<>();
        List<TextField> hintFields = new ArrayList<>();
        List<TextArea> lineFields = new ArrayList<>();
        for (ConversationObjective o : objectives) {
            TextField obj = field(o.getPosition() + ". What the learner must say (English)", o.getObjectiveEn());
            TextField hint = field("   Vocabulary hint", o.getVocabularyHint());
            TextArea line = area("   Persona asks (" + language + ")", o.getFallbackLine(), "60px");
            objectiveFields.add(obj);
            hintFields.add(hint);
            lineFields.add(line);
            form.add(obj, hint, line);
        }

        Button save = new Button("Save", e -> {
            try {
                c.setScenarioTitle(title.getValue().trim());
                c.setPersonaName(persona.getValue().trim());
                c.setPersonaDescription(description.getValue());
                c.setSceneNote(scene.getValue());
                c.setIntroLine(intro.getValue().trim());
                c.setIntroEnglish(introEn.getValue());
                c.setClosingLine(closing.getValue().trim());
                c.setClosingEnglish(closingEn.getValue());
                for (int i = 0; i < objectives.size(); i++) {
                    objectives.get(i).setObjectiveEn(objectiveFields.get(i).getValue().trim());
                    objectives.get(i).setVocabularyHint(hintFields.get(i).getValue());
                    objectives.get(i).setFallbackLine(lineFields.get(i).getValue().trim());
                }
                if (c.getScenarioTitle().isEmpty() || c.getPersonaName().isEmpty() || c.getIntroLine().isEmpty()
                        || c.getClosingLine().isEmpty() || objectives.stream().anyMatch(o -> o.getObjectiveEn().isEmpty()
                        || o.getFallbackLine().isEmpty())) {
                    Notification.show("Please fill in every required line.");
                    return;
                }
                library.saveEdits(c, objectives);
                Notification.show("Saved.");
                dialog.close();
                reload();
            } catch (RuntimeException ex) {
                Notification.show("Could not save the changes.");
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private static TextField field(String label, String value) {
        TextField f = new TextField(label);
        f.setValue(value == null ? "" : value);
        f.setWidthFull();
        return f;
    }

    private static TextArea area(String label, String value, String minHeight) {
        TextArea a = new TextArea(label);
        a.setValue(value == null ? "" : value);
        a.setWidthFull();
        a.setMinHeight(minHeight);
        return a;
    }
}
