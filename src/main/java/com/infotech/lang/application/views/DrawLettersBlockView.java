package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessBlockResult;
import com.infotech.lang.application.assessment.AssessmentMediaService;
import com.infotech.lang.application.assessment.AssessmentService;
import com.infotech.lang.application.assessment.BlockEngine;
import com.infotech.lang.application.assessment.MediaUrls;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.handwriting.DrawingPad;
import com.infotech.lang.application.handwriting.HandwritingJudge;
import com.infotech.lang.application.handwriting.LetterPicker;
import com.infotech.lang.application.handwriting.ReferenceImageService;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Block 2, draw letters: the student sees a letter (its picture and sound) and writes it on the pad. The old shape comparison
 * decides (see {@link HandwritingJudge}); no model is involved. A drawing that is not right gets one more try at the same
 * letter, which is not counted as a question; a second miss counts 0 and is flagged in the answer log for a teacher.
 * Pictures are not stored. {@link BlockEngine} decides how many letters are asked, as in the words block.
 */
@Route(value = "assessment/letters", layout = MainLayout.class)
@PageTitle("Letters | TestMyLanguage")
@PermitAll
public class DrawLettersBlockView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(DrawLettersBlockView.class);
    private static final String BLOCK_CODE = "DRAW_LETTERS";
    private static final int LEVEL = 1;

    private final AssessmentService assessment;
    private final AssessmentMediaService media;
    private final HandwritingJudge judge;
    private final ReferenceImageService referenceImages;
    private final String language;
    private final BlockEngine engine = new BlockEngine();

    private List<String> letters = List.of();
    private AssessBlockResult block;
    private String current;
    private int triesOnCurrent;
    private DrawingPad pad;
    private Button checkButton;
    private Paragraph message;
    private long shownAtMillis;

    public DrawLettersBlockView(SecurityService securityService,
                                UserRepository userRepository,
                                LetterPicker picker,
                                AssessmentService assessment,
                                AssessmentMediaService media,
                                HandwritingJudge judge,
                                ReferenceImageService referenceImages,
                                LanguageContext languageContext) {
        this.assessment = assessment;
        this.media = media;
        this.judge = judge;
        this.referenceImages = referenceImages;
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
            letters = picker.pick(language, engine.maxQuestions());
            if (letters.isEmpty()) {
                showMessage("There are no letters ready for this level yet.");
                return;
            }
            var attempt = assessment.currentOrNewAttempt(userId, language, LEVEL);
            block = assessment.startBlock(attempt.getId(), BLOCK_CODE, 2);
            showLetter();
        } catch (RuntimeException e) {
            logger.error("Could not start the letters block", e);
            showMessage("Sorry, we could not start this part. Please try again in a moment.");
        }
    }

    private void showLetter() {
        removeAll();
        current = letters.get(engine.asked());
        triesOnCurrent = 0;
        shownAtMillis = System.currentTimeMillis();
        add(new H2("Letters"), new H3("Letter " + (engine.asked() + 1)),
                new Paragraph("Write this letter on the pad, then tap Check."));

        referenceImages.referenceUrlFor(current).map(url -> MediaUrls.proxyUrl("image", url)).ifPresent(src -> {
            Image picture = new Image(src, "the letter to write");
            picture.getStyle().set("max-height", "160px").set("max-width", "100%");
            add(picture);
        });
        media.letterAudioBlob(language, current).ifPresent(blob -> {
            Button listen = new Button("Listen to the letter");
            // The sound starts inside the tap itself in the browser
            listen.getElement().executeJs(
                    "const url = $0; let audio = null;"
                            + "this.addEventListener('click', () => { if (audio) { audio.pause(); }"
                            + " audio = new Audio(url); audio.play(); });",
                    MediaUrls.forBlob("audio", AssessmentMediaService.AUDIO_CONTAINER, blob));
            add(listen);
        });

        pad = new DrawingPad();
        pad.addDrawingExportedListener(e -> submit(e.png(), e.isBlank()));
        message = new Paragraph();
        Button clear = new Button("Clear", e -> pad.clearPad());
        checkButton = new Button("Check", e -> pad.exportDrawing());
        add(pad, message, new HorizontalLayout(clear, checkButton));
    }

    private void submit(byte[] png, boolean blank) {
        if (blank || png == null) {
            Notification.show("Write the letter on the pad first.");
            return;
        }
        checkButton.setEnabled(false);
        try {
            HandwritingJudge.Verdict verdict = judge.judge(png, current);
            if (!verdict.correct() && triesOnCurrent == 0) {
                triesOnCurrent = 1;
                pad.clearPad();
                message.setText("Let's try that letter once more.");
                checkButton.setEnabled(true);
                return;
            }
            String log = (verdict.correct() ? "" : "FLAG unclear: ") + verdict.summary(current);
            int duration = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - shownAtMillis);
            assessment.recordAnswer(block.getId(), engine.asked() + 1, "letter:" + current, log, verdict.correct(), duration);
            engine.record(verdict.correct());
            if (engine.isComplete()) {
                assessment.finishBlock(block.getId(), engine.asked(), engine.correct(), engine.passed());
                showDone();
            } else {
                showLetter();
            }
        } catch (RuntimeException e) {
            logger.error("Could not check or save the drawing", e);
            checkButton.setEnabled(true);
            Notification.show("Sorry, that could not be saved. Please try again.");
        }
    }

    private void showDone() {
        removeAll();
        add(new H2("Letters finished"));
        add(new Paragraph("Thank you. Your answers are saved. Your results will be shown at the end of the assessment."));
        add(new Button("Continue to the audio words", e -> getUI().ifPresent(ui -> ui.navigate("assessment/audio-words"))));
        add(new Button("Back to the welcome page", e -> getUI().ifPresent(ui -> ui.navigate("welcome"))));
    }

    private void showMessage(String text) {
        removeAll();
        add(new H2("Letters"), new Paragraph(text));
    }

    private static UUID userIdOf(SecurityService securityService, UserRepository userRepository) {
        UserDetails user = securityService.getAuthenticatedUser();
        if (user == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }
}
