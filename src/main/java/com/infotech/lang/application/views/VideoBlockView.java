package com.infotech.lang.application.views;

import com.infotech.lang.application.ai.SpeechToText;
import com.infotech.lang.application.assessment.AssessBlockResult;
import com.infotech.lang.application.assessment.AssessmentMediaService;
import com.infotech.lang.application.assessment.AssessmentService;
import com.infotech.lang.application.assessment.BlockEngine;
import com.infotech.lang.application.assessment.MediaUrls;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.report.ReportService;
import com.infotech.lang.application.security.SecurityService;
import com.infotech.lang.application.translation.SpellingSuggester;
import com.infotech.lang.application.translation.TranslationGrader;
import com.infotech.lang.application.translation.TranslationVerdict;
import com.infotech.lang.application.video.VideoClip;
import com.infotech.lang.application.video.VideoLibrary;
import com.infotech.lang.application.video.VideoPicker;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Block 6, video. A short silent clip (or picture) plays and the student says what is happening in the tested language, typing (English
 * letters turn into script) or speaking, in the same answer box as the other parts. {@link TranslationGrader} judges the answer for
 * meaning against the clip's description (the clip is not re-sent), so another correct wording passes; spelling is reported separately and
 * never changes the point. {@link BlockEngine} decides how many clips are asked (4, then 4 more). A clip is never repeated for a student
 * while unseen clips remain. Correctness is not shown until the report (admins see it).
 */
@Route(value = "assessment/video", layout = MainLayout.class)
@PageTitle("Video | TestMyLanguage")
@PermitAll
public class VideoBlockView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger logger = LoggerFactory.getLogger(VideoBlockView.class);
    private static final String BLOCK_CODE = "VIDEO";
    private static final int LEVEL = 1;

    private final AssessmentService assessment;
    private final TranslationGrader grader;
    private final VideoPicker picker;
    private final VideoLibrary library;
    private final ReportService reportService;
    private final SpellingSuggester speller;
    private final SpeechToText speech;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final String language;
    private final BlockEngine engine = new BlockEngine();

    private List<VideoClip> clips = List.of();
    private AssessBlockResult block;
    private VideoClip current;
    private SmartAnswerBox answerBox;
    private Button send;
    private long shownAtMillis;
    private boolean adminTest;

    public VideoBlockView(SecurityService securityService, UserRepository userRepository, VideoPicker picker, VideoLibrary library,
                          AssessmentService assessment, TranslationGrader grader, ReportService reportService,
                          SpellingSuggester speller, SpeechToText speech, LanguageContext languageContext) {
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.picker = picker;
        this.library = library;
        this.assessment = assessment;
        this.grader = grader;
        this.reportService = reportService;
        this.speller = speller;
        this.speech = speech;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("760px");
        getStyle().set("margin", "0 auto");
    }

    /** An admin may add {@code ?test=1} to try the block with any verified clips, ignoring history. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (block != null) {
            return;
        }
        try {
            UUID userId = userIdOf();
            if (userId == null) {
                showMessage("Please sign in again to start.");
                return;
            }
            adminTest = MainLayout.isAdmin() && event.getLocation().getQueryParameters().getParameters().containsKey("test");
            clips = adminTest ? picker.pickForAdmin(language, LEVEL, engine.maxQuestions())
                    : picker.pick(userId, language, LEVEL, engine.maxQuestions());
            if (clips.size() < BlockEngine.FIRST_ROUND) {
                showMessage("There are not enough clips ready for this part yet.");
                return;
            }
            var attempt = assessment.currentOrNewAttempt(userId, language, LEVEL);
            block = assessment.startBlock(attempt.getId(), BLOCK_CODE, 6);
            showClip();
        } catch (RuntimeException e) {
            logger.error("Could not start the video block", e);
            showMessage("Sorry, we could not start this part. Please try again in a moment.");
        }
    }

    private void showClip() {
        removeAll();
        current = clips.get(engine.asked());
        shownAtMillis = System.currentTimeMillis();
        String name = "hindi".equalsIgnoreCase(language) ? "Hindi" : "Tamil";
        add(new H2("Video"), new H3("Clip " + (engine.asked() + 1)),
                new Paragraph("Watch, then say or write in " + name + " what is happening. You can watch again."));
        String url = MediaUrls.forBlob("video", AssessmentMediaService.VIDEO_CONTAINER, current.getBlobName());
        if (current.isVideo()) {
            add(new Html("<video src=\"" + url + "\" autoplay muted loop playsinline controls style=\"max-width:420px;width:100%;"
                    + "border-radius:12px\"></video>"));
        } else {
            add(new Html("<img src=\"" + url.replace("/video/", "/image/") + "\" alt=\"\" style=\"max-width:420px;width:100%;"
                    + "border-radius:12px\"/>"));
        }
        answerBox = new SmartAnswerBox(language, speller, speech);
        answerBox.setWidthFull();
        send = new Button("Send", e -> submit());
        add(answerBox, send);
    }

    private void submit() {
        String given = answerBox.text();
        if (given.isEmpty()) {
            Notification.show("Say or type your answer first.");
            return;
        }
        send.setEnabled(false);
        try {
            TranslationGrader.Grade grade = grader.grade(language, current.getEnglish(), current.getExpectedText(), given);
            if (grade.verdict() == TranslationVerdict.ERROR) {
                grade = grader.grade(language, current.getEnglish(), current.getExpectedText(), given);
            }
            boolean graderFailed = grade.verdict() == TranslationVerdict.ERROR;
            String spellingNote = grade.hasSpellingNote()
                    ? " {SPELLING " + grade.spelling() + (grade.corrected().isBlank() ? "" : ": " + grade.corrected()) + "}" : "";
            String log = (graderFailed ? "FLAG grader unavailable: " : "") + answerBox.logPrefix(given) + given
                    + " [" + grade.verdict() + "]" + spellingNote;
            int duration = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - shownAtMillis);
            assessment.recordAnswer(block.getId(), engine.asked() + 1, "clip:" + current.getId(), log, grade.correct(), duration);
            if (!adminTest) {
                library.markUsed(current.getId());
            }
            AdminFeedback.show(grade.correct(), grade.verdict() + (grade.usedModel() ? " (Meta: " + grade.reason() + ")" : " (exact match)")
                    + (grade.hasSpellingNote() ? " – spelling: " + grade.spelling() + " → " + grade.corrected() : "")
                    + " | expected: " + current.getExpectedText());
            engine.record(grade.correct());
            if (engine.isComplete() || engine.asked() >= clips.size()) {
                finishBlock();
            } else {
                showClip();
            }
        } catch (RuntimeException e) {
            logger.error("Could not grade or save the answer", e);
            send.setEnabled(true);
            Notification.show("Sorry, that answer could not be saved. Please try again.");
        }
    }

    private void finishBlock() {
        assessment.finishBlock(block.getId(), engine.asked(), engine.correct(), engine.passed());
        removeAll();
        add(new H2("Video finished"));
        add(new Paragraph("Thank you. Your answers are saved."));
        Long attemptId = block.getAttemptId();
        try {
            reportService.complete(attemptId);
            add(new Button("See your results", e -> getUI().ifPresent(ui -> ui.navigate("assessment/report",
                    QueryParameters.simple(Map.of("attempt", String.valueOf(attemptId)))))));
        } catch (RuntimeException e) {
            logger.error("Could not complete the attempt", e);
            add(new Paragraph("Your answers are saved, but the results could not be prepared right now."));
        }
        add(new Button("Back to the welcome page", e -> getUI().ifPresent(ui -> ui.navigate("welcome"))));
    }

    private void showMessage(String text) {
        removeAll();
        add(new H2("Video"), new Paragraph(text));
    }

    private UUID userIdOf() {
        UserDetails user = securityService.getAuthenticatedUser();
        return user == null ? null : userRepository.findByEmailIgnoreCase(user.getUsername()).map(User::getId).orElse(null);
    }
}
