package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessBlockResult;
import com.infotech.lang.application.assessment.AssessmentService;
import com.infotech.lang.application.assessment.BlockEngine;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.infotech.lang.application.translation.SentencePicker;
import com.infotech.lang.application.translation.TranslationGrader;
import com.infotech.lang.application.translation.TranslationItem;
import com.infotech.lang.application.translation.TranslationVerdict;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Block 4, translation: the student sees an English sentence and types its translation (with the device's own keyboard).
 * {@link TranslationGrader} judges it: an exact match is correct without a model, otherwise the Meta model judges the meaning and
 * a correct paraphrase passes. If the grader cannot be used it is tried once more; a second failure counts 0 and the answer
 * log starts with {@code FLAG}. One point per correct sentence; correctness is not shown until the final report.
 * {@link BlockEngine} decides how many sentences are asked (4, then 4 more, always ending after 8).
 */
@Route("assessment/translation")
@PageTitle("Translation | TestMyLanguage")
@PermitAll
public class TranslationBlockView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(TranslationBlockView.class);
    private static final String BLOCK_CODE = "TRANSLATION";
    private static final int LEVEL = 1;

    private final AssessmentService assessment;
    private final TranslationGrader grader;
    private final String language;
    private final BlockEngine engine = new BlockEngine();

    private List<TranslationItem> items = List.of();
    private AssessBlockResult block;
    private TranslationItem current;
    private TextArea answerBox;
    private Button checkButton;
    private long shownAtMillis;

    public TranslationBlockView(SecurityService securityService,
                                UserRepository userRepository,
                                SentencePicker picker,
                                AssessmentService assessment,
                                TranslationGrader grader,
                                LanguageContext languageContext) {
        this.assessment = assessment;
        this.grader = grader;
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
            items = picker.pick(language, engine.maxQuestions());
            if (items.isEmpty()) {
                showMessage("There are no sentences ready for this part yet.");
                return;
            }
            var attempt = assessment.currentOrNewAttempt(userId, language, LEVEL);
            block = assessment.startBlock(attempt.getId(), BLOCK_CODE, 4);
            showSentence();
        } catch (RuntimeException e) {
            logger.error("Could not start the translation block", e);
            showMessage("Sorry, we could not start this part. Please try again in a moment.");
        }
    }

    private void showSentence() {
        removeAll();
        current = items.get(engine.asked());
        shownAtMillis = System.currentTimeMillis();
        String name = "hindi".equalsIgnoreCase(language) ? "Hindi" : "Tamil";
        add(new H2("Translation"), new H3("Sentence " + (engine.asked() + 1)),
                new Paragraph("Write this sentence in " + name + "."),
                new Paragraph(current.english()));
        answerBox = new TextArea();
        answerBox.setPlaceholder("Type your answer here");
        answerBox.setWidthFull();
        answerBox.setMinHeight("100px");
        answerBox.setMaxLength(400);
        answerBox.getStyle().set("font-size", "1.3em");
        checkButton = new Button("Check", e -> submit());
        add(answerBox, checkButton);
    }

    private void submit() {
        String given = answerBox.getValue() == null ? "" : answerBox.getValue().trim();
        if (given.isEmpty()) {
            Notification.show("Type your answer first.");
            return;
        }
        checkButton.setEnabled(false);
        try {
            TranslationGrader.Grade grade = grader.grade(language, current.english(), current.expected(), given);
            if (grade.verdict() == TranslationVerdict.ERROR) {
                grade = grader.grade(language, current.english(), current.expected(), given);
            }
            boolean graderFailed = grade.verdict() == TranslationVerdict.ERROR;
            String log = (graderFailed ? "FLAG grader unavailable: " : "") + given + " [" + grade.verdict() + "]";
            int duration = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - shownAtMillis);
            assessment.recordAnswer(block.getId(), engine.asked() + 1, "sentence:" + current.id(), log, grade.correct(), duration);
            engine.record(grade.correct());
            if (engine.isComplete()) {
                assessment.finishBlock(block.getId(), engine.asked(), engine.correct(), engine.passed());
                showDone();
            } else {
                showSentence();
            }
        } catch (RuntimeException e) {
            logger.error("Could not grade or save the answer", e);
            checkButton.setEnabled(true);
            Notification.show("Sorry, that answer could not be saved. Please try again.");
        }
    }

    private void showDone() {
        removeAll();
        add(new H2("Translation finished"));
        add(new Paragraph("Thank you. Your answers are saved. Your results will be shown at the end of the assessment."));
        add(new Paragraph("The next part is not ready yet."));
        add(new Button("Back to the welcome page", e -> getUI().ifPresent(ui -> ui.navigate("welcome"))));
    }

    private void showMessage(String text) {
        removeAll();
        add(new H2("Translation"), new Paragraph(text));
    }

    private static UUID userIdOf(SecurityService securityService, UserRepository userRepository) {
        UserDetails user = securityService.getAuthenticatedUser();
        if (user == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }
}
