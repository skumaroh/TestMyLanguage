package com.infotech.lang.application.views;

import com.infotech.lang.application.ai.SpeechToText;
import com.infotech.lang.application.assessment.AssessBlockResult;
import com.infotech.lang.application.assessment.AssessmentService;
import com.infotech.lang.application.assessment.BlockEngine;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.conversation.Conversation;
import com.infotech.lang.application.conversation.ConversationObjective;
import com.infotech.lang.application.conversation.ConversationPicker;
import com.infotech.lang.application.conversation.ConversationTurnGrader;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.infotech.lang.application.translation.SpellingSuggester;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Block 5, conversation. The student meets an imagined person from the verified conversation library (one conversation per test,
 * never one they have had before). The persona opens; the student answers eight possible objectives in turn, typing (English letters
 * turn into Tamil/Hindi) or speaking. {@link ConversationTurnGrader} judges each reply and writes the persona's next line; only MET
 * earns the point. {@link BlockEngine} decides how many objectives are asked (4, then 4 more when under 75%). "I'm done" or a quiet
 * period ends the conversation early with the rest counting 0. The persona then closes with the conversation's closing line.
 * Correctness is not shown to the student (admins see it).
 */
@Route(value = "assessment/conversation", layout = MainLayout.class)
@PageTitle("Conversation | TestMyLanguage")
@PermitAll
public class ConversationBlockView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ConversationBlockView.class);
    private static final String BLOCK_CODE = "CONVERSATION";
    private static final int LEVEL = 1;
    private static final int QUIET_SECONDS = 45;
    private static final int WARN_SECONDS = 10;

    private final AssessmentService assessment;
    private final ConversationTurnGrader grader;
    private final String language;
    private final BlockEngine engine = new BlockEngine();
    private final SpellingSuggester speller;
    private final SpeechToText speech;

    private ConversationPicker.Picked picked;
    private AssessBlockResult block;
    private final List<ConversationTurnGrader.Line> history = new ArrayList<>();
    private String asked = "";
    private String askedEnglish = "";
    private boolean hintUsed;
    private long shownAtMillis;
    private boolean finished;

    private Div transcript;
    private Div controls;
    private SmartAnswerBox answerBox;
    private IdleGuard guard;
    private Button send;
    private Paragraph glossLine;

    public ConversationBlockView(SecurityService securityService,
                                 UserRepository userRepository,
                                 ConversationPicker picker,
                                 AssessmentService assessment,
                                 ConversationTurnGrader grader,
                                 SpellingSuggester speller,
                                 SpeechToText speech,
                                 LanguageContext languageContext) {
        this.assessment = assessment;
        this.grader = grader;
        this.speller = speller;
        this.speech = speech;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("760px");
        getStyle().set("margin", "0 auto");
        try {
            UUID userId = userIdOf(securityService, userRepository);
            if (userId == null) {
                showMessage("Please sign in again to start.");
                return;
            }
            var found = picker.pick(userId, language, LEVEL);
            if (found.isEmpty()) {
                showMessage("There is no conversation ready for this part yet.");
                return;
            }
            picked = found.get();
            var attempt = assessment.currentOrNewAttempt(userId, language, LEVEL);
            block = assessment.startBlock(attempt.getId(), BLOCK_CODE, 5);
            showConversation();
        } catch (RuntimeException e) {
            logger.error("Could not start the conversation block", e);
            showMessage("Sorry, we could not start this part. Please try again in a moment.");
        }
    }

    private void showConversation() {
        removeAll();
        Conversation c = picked.conversation();
        add(new H2("Conversation"));
        Paragraph note = new Paragraph(nz(c.getSceneNote()).isBlank()
                ? "Imagine you meet " + c.getPersonaName() + ". Answer in your imagination: use made-up names and details."
                : c.getSceneNote());
        note.getStyle().set("font-weight", "600");
        add(note, new Paragraph("Please do not type your real name, address or phone number. If you want to stop, press \"I'm done\"."));
        transcript = new Div();
        transcript.setWidthFull();
        add(transcript);

        answerBox = new SmartAnswerBox(language, speller, speech);
        answerBox.setWidthFull();
        send = new Button("Send", e -> submit());
        Button done = new Button("I'm done", e -> endEarly("DONE"));
        done.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        guard = new IdleGuard(() -> endEarly("TIMEOUT"), QUIET_SECONDS, WARN_SECONDS);
        controls = new Div(answerBox, guard, new HorizontalLayout(send, done));
        controls.setWidthFull();
        add(controls);

        history.add(new ConversationTurnGrader.Line(true, c.getIntroLine()));
        showPersona(c.getIntroLine(), c.getIntroEnglish());
        guard.start();
    }

    private void showPersona(String line, String english) {
        asked = line;
        askedEnglish = nz(english);
        hintUsed = false;
        shownAtMillis = System.currentTimeMillis();
        Div bubble = bubble(picked.conversation().getPersonaName() + ": " + line, true);
        transcript.add(bubble);
        glossLine = new Paragraph();
        glossLine.getStyle().set("font-size", "var(--lumo-font-size-s)").set("margin", "0 0 var(--lumo-space-s) 0");
        Button gloss = new Button("Show English", e -> {
            hintUsed = true;
            glossLine.setText(askedEnglish.isBlank() ? "(no English is available for this line)" : askedEnglish);
            e.getSource().setVisible(false);
        });
        gloss.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        transcript.add(new Div(gloss, glossLine));
    }

    private static Div bubble(String text, boolean persona) {
        Div d = new Div();
        d.setText(text);
        d.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)").set("margin", "var(--lumo-space-xs) 0")
                .set("border-radius", "var(--lumo-border-radius-l)").set("max-width", "85%").set("font-size", "1.2em")
                .set("background", persona ? "var(--lumo-contrast-10pct)" : "var(--lumo-primary-color-10pct)")
                .set("margin-left", persona ? "0" : "auto").set("width", "fit-content");
        return d;
    }

    private void submit() {
        if (finished) {
            return;
        }
        String given = answerBox.text();
        if (given.isEmpty()) {
            Notification.show("Say or type your answer first.");
            return;
        }
        send.setEnabled(false);
        guard.stop();
        try {
            int index = engine.asked();
            ConversationObjective current = picked.objectives().get(index);
            ConversationObjective next = index + 1 < picked.objectives().size() ? picked.objectives().get(index + 1) : null;
            ConversationTurnGrader.Turn turn = grader.grade(language, picked.conversation(), current, next, asked, history, given);
            if ("ERROR".equals(turn.verdict())) {
                turn = grader.grade(language, picked.conversation(), current, next, asked, history, given);
            }
            boolean graderFailed = "ERROR".equals(turn.verdict());
            String prefix = answerBox.logPrefix(given) + (hintUsed ? "HINT " : "");
            String flag = index == 0 && !picked.flag().isEmpty() ? "FLAG " + picked.flag() + " conversation: " : "";
            String spelling = turn.hasSpellingNote()
                    ? " {SPELLING " + turn.spelling() + (turn.corrected().isBlank() ? "" : ": " + turn.corrected()) + "}" : "";
            String reply = !turn.reply().isBlank() ? turn.reply() : (next != null ? next.getFallbackLine() : "");
            String log = (graderFailed ? "FLAG grader unavailable: " : "") + flag + prefix + given + " [" + turn.verdict()
                    + (turn.reason().isBlank() ? "" : ": " + turn.reason()) + "]" + spelling
                    + " | ASKED: " + asked + (reply.isBlank() ? "" : " | NEXT: " + reply);
            int duration = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - shownAtMillis);
            assessment.recordAnswer(block.getId(), index + 1, itemRef(index + 1), log, turn.met(), duration);
            AdminFeedback.show(turn.met(), turn.verdict() + (turn.reason().isBlank() ? "" : " (" + turn.reason() + ")")
                    + (turn.hasSpellingNote() ? " – spelling: " + turn.spelling() + " → " + turn.corrected() : ""));
            engine.record(turn.met());
            history.add(new ConversationTurnGrader.Line(false, given));
            transcript.add(bubble(given, false));
            if (engine.isComplete()) {
                finishBlock();
            } else {
                history.add(new ConversationTurnGrader.Line(true, reply));
                showPersona(reply, turn.replyEnglish());
                answerBox.clear();
                send.setEnabled(true);
                guard.start();
            }
        } catch (RuntimeException e) {
            logger.error("Could not grade or save the answer", e);
            send.setEnabled(true);
            guard.start();
            Notification.show("Sorry, that answer could not be saved. Please try again.");
        }
    }

    /** "I'm done" or a quiet period: the objectives not yet answered count 0, then the conversation closes. */
    private void endEarly(String why) {
        if (finished || block == null) {
            return;
        }
        guard.stop();
        try {
            while (!engine.isComplete()) {
                int index = engine.asked();
                assessment.recordAnswer(block.getId(), index + 1, itemRef(index + 1), "[SKIPPED: " + why + "]", false, 0);
                engine.record(false);
            }
            finishBlock();
        } catch (RuntimeException e) {
            logger.error("Could not end the conversation", e);
            Notification.show("Sorry, something went wrong. Please try again.");
        }
    }

    private String itemRef(int position) {
        return "conv:" + picked.conversation().getId() + ":" + position;
    }

    private void finishBlock() {
        finished = true;
        guard.stop();
        assessment.finishBlock(block.getId(), engine.asked(), engine.correct(), engine.passed());
        remove(controls);
        Conversation c = picked.conversation();
        transcript.add(bubble(c.getPersonaName() + ": " + c.getClosingLine(), true));
        add(new H2("Conversation finished"));
        add(new Paragraph("Thank you. Your answers are saved. Your results will be shown at the end of the assessment."));
        add(new Paragraph("The next part is not ready yet."));
        add(new Button("Back to the welcome page", e -> getUI().ifPresent(ui -> ui.navigate("welcome"))));
    }

    private void showMessage(String text) {
        removeAll();
        add(new H2("Conversation"), new Paragraph(text));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static UUID userIdOf(SecurityService securityService, UserRepository userRepository) {
        UserDetails user = securityService.getAuthenticatedUser();
        if (user == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }
}
