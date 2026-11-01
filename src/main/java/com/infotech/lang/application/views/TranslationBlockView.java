package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.AssessBlockResult;
import com.infotech.lang.application.assessment.AssessmentService;
import com.infotech.lang.application.assessment.BlockEngine;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.security.SecurityService;
import com.infotech.lang.application.translation.SentencePicker;
import com.infotech.lang.application.translation.SpellingRun;
import com.infotech.lang.application.translation.SpellingSuggester;
import com.infotech.lang.application.translation.TypedWord;
import com.infotech.lang.application.translation.TileQuestion;
import com.infotech.lang.application.translation.TranslationGrader;
import com.infotech.lang.application.translation.TranslationItem;
import com.infotech.lang.application.translation.TranslationVerdict;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
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
 * Block 4, translation: the student sees an English sentence (from a hand-written list) and answers it in one of two ways, half
 * of the questions each: a word-tile question (tap the words in order; the words of the stored answer plus a few decoys) at the
 * 1st, 3rd ... question, and a typed question at the others (English letters turn into Tamil or Hindi as the student types;
 * the device's own keyboard works too). A tile order that matches the stored answer or an accepted form is correct without a
 * model; any other tile order, and every typed answer, is judged for meaning by {@link TranslationGrader} (a correct paraphrase,
 * another word order, or an answer in English letters passes). If the grader cannot be used it is tried once more; a second
 * failure counts 0 and the answer log starts with {@code FLAG}. One point per correct sentence; correctness is not shown until
 * the final report (admins see it after each answer). {@link BlockEngine} decides how many sentences are asked (4, then 4 more).
 */
@Route(value = "assessment/translation", layout = MainLayout.class)
@PageTitle("Translation | TestMyLanguage")
@JsModule("./components/transliterate.js")
@PermitAll
public class TranslationBlockView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(TranslationBlockView.class);
    private static final String BLOCK_CODE = "TRANSLATION";
    private static final int LEVEL = 1;

    private final AssessmentService assessment;
    private final TranslationGrader grader;
    private final String language;
    private final BlockEngine engine = new BlockEngine();

    private final SentencePicker picker;
    private final SpellingSuggester speller;
    private Div suggestionRow;
    private String lastSuggestedFor = "";
    private int suggestionsUsed;
    private final ObjectMapper json = new ObjectMapper();

    private List<TranslationItem> items = List.of();
    private List<TranslationItem> pool = List.of();
    private final List<String> tapped = new ArrayList<>();
    private Div tappedRow;
    private FlexLayout tileRow;
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
                                SpellingSuggester speller,
                                LanguageContext languageContext) {
        this.assessment = assessment;
        this.grader = grader;
        this.picker = picker;
        this.speller = speller;
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
            pool = picker.allFor(language);
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
        boolean tiles = current.hasAnswer() && engine.asked() % 2 == 0;
        String name = "hindi".equalsIgnoreCase(language) ? "Hindi" : "Tamil";
        add(new H2("Translation"), new H3("Sentence " + (engine.asked() + 1)),
                new Paragraph((tiles ? "Tap the " + name + " words in the right order to say this sentence."
                        : "Write this sentence in " + name + ".")),
                new Paragraph(current.english()));
        if (tiles) {
            showTiles();
        } else {
            showTyping(name);
        }
    }

    private void showTyping(String name) {
        tapped.clear();
        answerBox = new TextArea();
        answerBox.setPlaceholder("Type your answer here");
        answerBox.setWidthFull();
        answerBox.setMinHeight("100px");
        answerBox.setMaxLength(400);
        answerBox.getStyle().set("font-size", "1.3em");
        Paragraph hint = new Paragraph("Tip: type " + name + " words in English letters (for example "
                + ("hindi".equalsIgnoreCase(language) ? "\"mai paanee peetaa hoon\"" : "\"naan thanneer kudikkiren\"")
                + ") and they turn into " + name + " letters. When you pause, Meta may suggest a better spelling; tap it to use it. "
                + ("hindi".equalsIgnoreCase(language) ? "Use \"aa\", \"ee\", \"oo\" for long vowels. "
                        : "Use \"aa\", \"ee\", \"oo\" for long vowels and \"ae\" for ஏ. ")
                + "You can also use your own " + name + " keyboard.");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        checkButton = new Button("Check", e -> submit());
        suggestionRow = new Div();
        suggestionRow.getStyle().set("min-height", "2.5em");
        suggestionsUsed = 0;
        lastSuggestedFor = "";
        answerBox.setValueChangeMode(ValueChangeMode.LAZY);
        answerBox.setValueChangeTimeout(900);
        // the suggestion buttons stay while the learner keeps typing; they are recomputed after each pause
        answerBox.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                offerSpelling();
            }
        });
        add(answerBox, suggestionRow, hint, checkButton);
        String tag = "hindi".equalsIgnoreCase(language) ? "hindi" : "tamil";
        answerBox.getElement().executeJs("window.tmlTransliterate && window.tmlTransliterate.attach(this, $0)", tag);
    }

    /**
     * After the learner pauses, asks Meta for the best script spelling of each word typed in English letters and offers one
     * button per word (or run of neighbouring words). Tapping a button replaces exactly those words and nothing else; the text is
     * never changed unless the learner taps.
     */
    private void offerSpelling() {
        answerBox.getElement().executeJs("return this.getAttribute('data-words') || '[]'").then(String.class, raw -> {
            List<TypedWord> typed = readTypedWords(raw);
            String key = typed.stream().map(w -> w.raw() + "=" + w.shown() + "@" + w.start()).reduce("", (a, b) -> a + "|" + b);
            if (typed.isEmpty()) {
                suggestionRow.removeAll();
                lastSuggestedFor = "";
                return;
            }
            if (key.equals(lastSuggestedFor)) {
                return;
            }
            lastSuggestedFor = key;
            try {
                List<SpellingRun> runs = SpellingRun.group(typed, speller.suggestWords(language, typed));
                showRuns(new ArrayList<>(runs));
            } catch (RuntimeException ex) {
                logger.warn("Spelling help failed", ex);
            }
        });
    }

    private List<TypedWord> readTypedWords(String raw) {
        List<TypedWord> words = new ArrayList<>();
        try {
            for (JsonNode n : json.readTree(raw == null ? "[]" : raw)) {
                words.add(new TypedWord(n.path("raw").asText(""), n.path("shown").asText(""), n.path("start").asInt(),
                        n.path("end").asInt()));
            }
        } catch (Exception e) {
            return List.of();
        }
        return words;
    }

    private void showRuns(List<SpellingRun> runs) {
        suggestionRow.removeAll();
        if (runs.isEmpty()) {
            return;
        }
        Span label = new Span("Did you mean: ");
        label.getStyle().set("margin-right", "var(--lumo-space-s)");
        suggestionRow.add(label);
        for (SpellingRun run : runs) {
            Button use = new Button(run.suggestion(), click -> applyRun(run, runs));
            use.getStyle().set("margin-right", "var(--lumo-space-xs)").set("margin-bottom", "var(--lumo-space-xs)")
                    .set("min-height", "2.5em");
            suggestionRow.add(use);
        }
        if (runs.size() > 1) {
            Button all = new Button("Use all", click -> applyAll(runs));
            all.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);
            suggestionRow.add(all);
        }
    }

    /** Replaces one suggestion's span in the browser box; the other buttons are shifted or refreshed. */
    private void applyRun(SpellingRun run, List<SpellingRun> runs) {
        answerBox.getElement().executeJs("return this.__tmlReplace ? this.__tmlReplace($0, $1, $2, $3) : false",
                run.start(), run.end(), run.currentText(), run.suggestion()).then(Boolean.class, done -> {
                    if (Boolean.TRUE.equals(done)) {
                        suggestionsUsed++;
                        int delta = run.suggestion().length() - (run.end() - run.start());
                        runs.remove(run);
                        List<SpellingRun> shifted = new ArrayList<>();
                        for (SpellingRun other : runs) {
                            shifted.add(other.start() >= run.end()
                                    ? new SpellingRun(other.start() + delta, other.end() + delta, other.currentText(), other.suggestion())
                                    : other);
                        }
                        lastSuggestedFor = "";
                        showRuns(shifted);
                    } else {
                        // the text under the button has changed since it was made: look again
                        lastSuggestedFor = "";
                        offerSpelling();
                    }
                });
    }

    /** Applies every suggestion, last first so earlier positions stay valid. */
    private void applyAll(List<SpellingRun> runs) {
        List<SpellingRun> sorted = new ArrayList<>(runs);
        sorted.sort((a, b) -> Integer.compare(b.start(), a.start()));
        StringBuilder script = new StringBuilder("let ok = true; ");
        for (SpellingRun r : sorted) {
            script.append("ok = this.__tmlReplace(").append(r.start()).append(", ").append(r.end()).append(", ")
                    .append(json.valueToTree(r.currentText())).append(", ").append(json.valueToTree(r.suggestion()))
                    .append(") && ok; ");
        }
        script.append("return ok;");
        answerBox.getElement().executeJs(script.toString()).then(Boolean.class, done -> {
            suggestionsUsed += sorted.size();
            suggestionRow.removeAll();
            lastSuggestedFor = "";
            if (!Boolean.TRUE.equals(done)) {
                offerSpelling();
            }
        });
    }

    private void showTiles() {
        answerBox = null;
        tapped.clear();
        TileQuestion question = TileQuestion.build(current, pool);
        tappedRow = new Div();
        tappedRow.getStyle().set("min-height", "3em").set("width", "100%").set("box-sizing", "border-box")
                .set("border", "2px dashed var(--lumo-contrast-30pct)").set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-s)").set("font-size", "1.3em");
        tileRow = new FlexLayout();
        tileRow.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        tileRow.getStyle().set("gap", "var(--lumo-space-s)");
        for (String word : question.tiles()) {
            Button tile = new Button(word);
            tile.getStyle().set("font-size", "1.2em");
            tile.addClickListener(e -> {
                tapped.add(word);
                tile.setEnabled(false);
                refreshTapped();
            });
            tileRow.add(tile);
        }
        Button undo = new Button("Undo", e -> undoTile());
        checkButton = new Button("Check", e -> submit());
        add(tappedRow, tileRow, new Div(undo, checkButton));
        refreshTapped();
    }

    private void refreshTapped() {
        tappedRow.setText(String.join(" ", tapped));
    }

    private void undoTile() {
        if (tapped.isEmpty()) {
            return;
        }
        String last = tapped.remove(tapped.size() - 1);
        tileRow.getChildren().filter(c -> c instanceof Button b && b.getText().equals(last) && !b.isEnabled())
                .findFirst().ifPresent(c -> ((Button) c).setEnabled(true));
        refreshTapped();
    }

    private void submit() {
        boolean tiles = answerBox == null;
        String given = tiles ? String.join(" ", tapped) : (answerBox.getValue() == null ? "" : answerBox.getValue().trim());
        if (given.isEmpty()) {
            Notification.show(tiles ? "Tap the words first." : "Type your answer first.");
            return;
        }
        checkButton.setEnabled(false);
        try {
            TranslationGrader.Grade grade;
            if (tiles && TileQuestion.matches(current, tapped)) {
                grade = new TranslationGrader.Grade(true, TranslationVerdict.SAME_MEANING, "", false, 0);
            } else {
                grade = grader.grade(language, current.english(), current.expected(), given);
                if (grade.verdict() == TranslationVerdict.ERROR) {
                    grade = grader.grade(language, current.english(), current.expected(), given);
                }
            }
            boolean graderFailed = grade.verdict() == TranslationVerdict.ERROR;
            String mode = tiles ? "TILES " : (suggestionsUsed > 0 ? "SPELLING-HELP " + suggestionsUsed + " " : "");
            String spellingNote = grade.hasSpellingNote()
                    ? " {SPELLING " + grade.spelling() + (grade.corrected().isBlank() ? "" : ": " + grade.corrected()) + "}" : "";
            String log = (graderFailed ? "FLAG grader unavailable: " : "") + mode + given + " [" + grade.verdict() + "]" + spellingNote;
            int duration = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - shownAtMillis);
            assessment.recordAnswer(block.getId(), engine.asked() + 1, "sentence:" + current.id(), log, grade.correct(), duration);
            AdminFeedback.show(grade.correct(), mode + grade.verdict()
                    + (grade.usedModel() ? " (Meta: " + grade.reason() + ")" : " (exact match)")
                    + (grade.hasSpellingNote() ? " – spelling: " + grade.spelling() + " → " + grade.corrected() : ""));
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
