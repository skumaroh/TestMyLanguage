package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessBlockResult;
import com.infotech.lang.application.assessment.AssessmentMediaService;
import com.infotech.lang.application.assessment.AssessmentService;
import com.infotech.lang.application.assessment.BlockEngine;
import com.infotech.lang.application.assessment.MediaUrls;
import com.infotech.lang.application.assessment.WordQuestion;
import com.infotech.lang.application.assessment.WordQuestionService;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Block 3, audio words: the student hears a word and builds it from letter tiles. Nothing else is shown: no prompt text and no
 * picture. Only words that have a recording are used, and words already asked in this attempt are avoided. One point per correct
 * word, none for a wrong one; correctness is not shown until the final report. {@link BlockEngine} decides how many words.
 */
@Route(value = "assessment/audio-words", layout = MainLayout.class)
@PageTitle("Audio words | TestMyLanguage")
@PermitAll
public class AudioWordsBlockView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(AudioWordsBlockView.class);
    private static final String BLOCK_CODE = "AUDIO_WORDS";
    private static final int LEVEL = 1;

    private final AssessmentService assessment;
    private final AssessmentMediaService media;
    private final String language;
    private final BlockEngine engine = new BlockEngine();
    private final StringBuilder built = new StringBuilder();

    private List<WordQuestion> questions = List.of();
    private AssessBlockResult block;
    private WordQuestion current;
    private TextField answerField;
    private Button checkButton;
    private long shownAtMillis;

    public AudioWordsBlockView(SecurityService securityService,
                          UserRepository userRepository,
                          WordQuestionService wordQuestions,
                          AssessmentService assessment,
                          AssessmentMediaService media,
                          LanguageContext languageContext) {
        this.assessment = assessment;
        this.media = media;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");
        try {
            UUID userId = userIdOf(securityService, userRepository);
            if (userId == null) {
                showMessage("Please sign in again to start.");
                return;
            }
            var attempt = assessment.currentOrNewAttempt(userId, language, LEVEL);
            questions = wordQuestions.pickWithAudio(engine.maxQuestions(), LEVEL, assessment.itemRefsInAttempt(attempt.getId()));
            if (questions.isEmpty()) {
                showMessage("There are no recorded words ready for this level yet.");
                return;
            }
            block = assessment.startBlock(attempt.getId(), BLOCK_CODE, 3);
            showQuestion();
        } catch (RuntimeException e) {
            logger.error("Could not start the audio words block", e);
            showMessage("Sorry, we could not start this part. Please try again in a moment.");
        }
    }

    private void showQuestion() {
        removeAll();
        current = questions.get(engine.asked());
        built.setLength(0);
        shownAtMillis = System.currentTimeMillis();

        add(new H2("Audio words"), new H3("Question " + (engine.asked() + 1)),
                new Paragraph("Listen to the word, then build it with the letters."));
        String audioSrc = MediaUrls.proxyUrl("audio", current.audioUrl());
        if (audioSrc != null) {
            Button listen = new Button("Listen to the word");
            // Plays inside the click itself in the browser (some browsers refuse sound started after a server round trip)
            listen.getElement().executeJs(
                    "const url = $0; let audio = null;"
                            + "this.addEventListener('click', () => { if (audio) { audio.pause(); }"
                            + " audio = new Audio(url); audio.play(); });",
                    audioSrc);
            add(listen);
        }

        answerField = new TextField();
        answerField.setPlaceholder("Your word");
        answerField.setReadOnly(true);
        answerField.setWidthFull();
        answerField.getStyle().set("font-size", "1.6em");
        add(answerField);

        Div tiles = new Div();
        tiles.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "var(--lumo-space-s)")
                .set("justify-content", "center");
        for (String tile : current.tiles()) {
            Button button = new Button(tile, e -> {
                built.append(tile);
                answerField.setValue(built.toString());
            });
            button.getStyle().set("font-size", "1.4em").set("min-width", "3em");
            media.letterAudioBlob(language, tile).ifPresent(blob -> button.getElement().executeJs(
                    // The letter's sound starts inside the tap itself in the browser
                    "const url = $0; let audio = null;"
                            + "this.addEventListener('click', () => { if (audio) { audio.pause(); }"
                            + " audio = new Audio(url); audio.play(); });",
                    MediaUrls.forBlob("audio", AssessmentMediaService.AUDIO_CONTAINER, blob)));
            tiles.add(button);
        }
        add(tiles);

        Button clear = new Button("Clear", e -> {
            built.setLength(0);
            answerField.setValue("");
        });
        checkButton = new Button("Check", e -> submit());
        add(new Div(clear, checkButton));
    }

    private void submit() {
        String given = built.toString();
        if (given.isBlank()) {
            Notification.show("Tap the letters to build the word.");
            return;
        }
        checkButton.setEnabled(false);
        try {
            boolean correct = WordQuestionService.isCorrect(given, current.answer());
            int duration = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - shownAtMillis);
            assessment.recordAnswer(block.getId(), engine.asked() + 1, "asset:" + current.id(), given, correct, duration);
            AdminFeedback.show(correct, correct ? "" : "expected " + current.answer());
            engine.record(correct);
            if (engine.isComplete()) {
                assessment.finishBlock(block.getId(), engine.asked(), engine.correct(), engine.passed());
                showDone();
            } else {
                showQuestion();
            }
        } catch (RuntimeException e) {
            logger.error("Could not save the answer", e);
            checkButton.setEnabled(true);
            Notification.show("Sorry, that answer could not be saved. Please try again.");
        }
    }

    private void showDone() {
        removeAll();
        add(new H2("Audio words finished"));
        add(new Paragraph("Thank you. Your answers are saved. Your results will be shown at the end of the assessment."));
        add(new Button("Continue to the translation", e -> getUI().ifPresent(ui -> ui.navigate("assessment/translation"))));
        add(new Button("Back to the welcome page", e -> getUI().ifPresent(ui -> ui.navigate("welcome"))));
    }

    private void showMessage(String text) {
        removeAll();
        add(new H2("Audio words"), new Paragraph(text));
    }

    private static UUID userIdOf(SecurityService securityService, UserRepository userRepository) {
        UserDetails user = securityService.getAuthenticatedUser();
        if (user == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }
}
