package com.infotech.lang.application.views;

import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.conversation.Conversation;
import com.infotech.lang.application.conversation.ConversationLibrary;
import com.infotech.lang.application.conversation.ConversationObjective;
import com.infotech.lang.application.conversation.PersonaLineReview;
import com.infotech.lang.application.conversation.PersonaVoice;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.infotech.lang.application.voice.SpeechRegistry;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
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
    private final PersonaLineReview lineReview;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final String language;
    private final transient SpeechRegistry speechRegistry;

    private final Grid<Conversation> grid = new Grid<>(Conversation.class, false);
    private final Div preview = new Div();
    private final Button verify = new Button("Verify");
    private final Button edit = new Button("Edit");
    private final Button retire = new Button("Retire");

    public ConversationAdminView(ConversationLibrary library, PersonaLineReview lineReview, SecurityService securityService, UserRepository userRepository,
                                 LanguageContext languageContext, SpeechRegistry speechRegistry) {
        this.library = library;
        this.lineReview = lineReview;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.language = languageContext.getLanguage();
        this.speechRegistry = speechRegistry;

        add(new H2("Conversations (" + language + ")"));
        add(new Paragraph("Meta writes imagined everyday conversations as drafts. Read each one, fix anything odd in the "
                + language + " lines, and verify it. Only verified conversations are used in a test; a learner never gets "
                + "the same conversation twice. Retired conversations are kept but never used. Every persona must be male, because the voice is male: "
                + "retire any conversation with a female persona (such as a grandmother) and write new drafts."));

        grid.addColumn(Conversation::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Conversation::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(Conversation::getScenarioTitle).setHeader("Scenario").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Conversation::getPersonaName).setHeader("Persona").setAutoWidth(true);
        // the speech voice is male, so a persona set to female (or a female-looking one) must be retired or rewritten
        grid.addColumn(c -> "FEMALE".equals(PersonaVoice.of(c).gender()) ? "⚠ female: retire" : PersonaVoice.of(c).describe())
                .setHeader("Voice").setAutoWidth(true);
        grid.addColumn(Conversation::getLevel).setHeader("Level").setAutoWidth(true);
        grid.addColumn(Conversation::getDifficulty).setHeader("Difficulty").setAutoWidth(true);
        grid.addColumn(Conversation::getSource).setHeader("Source").setAutoWidth(true);
        grid.addColumn(Conversation::getTimesUsed).setHeader("Used").setAutoWidth(true);
        grid.setHeight("320px");
        grid.asSingleSelect().addValueChangeListener(e -> showSelection(e.getValue()));

        preview.setWidthFull();
        preview.setMinHeight("260px");
        preview.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)").set("box-sizing", "border-box");

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
        add(grid, new H4("Selected conversation"), preview, new HorizontalLayout(verify, edit, retire));
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
        preview.removeAll();
        if (c == null) {
            return;
        }
        preview.add(line(c.getScenarioTitle() + "  [" + c.getStatus() + ", " + c.getDifficulty() + "]", null, true));
        preview.add(line("Persona: " + c.getPersonaName() + " – " + nullToEmpty(c.getPersonaDescription()), null, false));
        preview.add(line("Voice: " + PersonaVoice.of(c).describe() + " (set \"gender\" and \"age_band\" in the persona box to change it)", null, false));
        preview.add(line("Note to the learner: " + nullToEmpty(c.getSceneNote()), null, false));
        preview.add(gap());
        // every line in the tested language has its Listen button right after it, on the same line
        preview.add(line("OPENING: ", c.getIntroLine(), true));
        preview.add(line("   (" + nullToEmpty(c.getIntroEnglish()) + ")", null, false));
        preview.add(gap());
        for (ConversationObjective o : library.objectivesOf(c.getId())) {
            preview.add(line(o.getPosition() + ". " + o.getObjectiveEn(), null, true));
            preview.add(line("   hint: ", o.getVocabularyHint(), false));
            preview.add(line("   persona asks: ", o.getFallbackLine(), false));
        }
        addLiveLines(c.getId());
        preview.add(gap());
        preview.add(line("CLOSING: ", c.getClosingLine(), true));
        preview.add(line("   (" + nullToEmpty(c.getClosingEnglish()) + ")", null, false));
    }

    /** The live lines Meta wrote in real attempts, so a good one can be copied into the stored objective. */
    private void addLiveLines(Long id) {
        preview.add(gap());
        try {
            List<PersonaLineReview.LiveLine> lines = lineReview.liveLines(id);
            if (lines.isEmpty()) {
                preview.add(line("LIVE LINES FROM META: none yet (they appear after learners or admins try this conversation).", null, true));
                return;
            }
            preview.add(line("LIVE LINES FROM META (what the persona actually said in attempts; copy a good one into \"persona asks\" above):",
                    null, true));
            for (PersonaLineReview.LiveLine l : lines) {
                preview.add(line("   after goal " + l.position() + " (x" + l.times() + "): ", l.next(), false));
                preview.add(line("         answering: " + l.asked(), null, false));
            }
        } catch (RuntimeException e) {
            preview.add(line("LIVE LINES FROM META: could not be read.", null, true));
        }
    }

    /**
     * One line of the preview: {@code label} as plain text, then (when given) the line in the tested language followed directly by
     * its Listen button on the same line.
     */
    private Div line(String label, String spoken, boolean bold) {
        Div d = new Div();
        d.getStyle().set("white-space", "pre-wrap").set("padding", "2px 0");
        Span l = new Span(label);
        if (bold) {
            l.getStyle().set("font-weight", "600");
        }
        d.add(l);
        if (spoken != null && !spoken.isBlank()) {
            d.add(new Span(spoken), new Span(" "), new SpeakButton(spoken, language, speechRegistry));
        }
        return d;
    }

    private static Div gap() {
        Div d = new Div();
        d.setHeight("0.6em");
        return d;
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
        TextArea description = area("Who the persona is (English text, or a JSON profile of the persona)", c.getPersonaDescription(), "220px");
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
