package com.infotech.lang.application.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotech.lang.application.ai.SpeechToText;
import com.infotech.lang.application.translation.SpellingRun;
import com.infotech.lang.application.translation.SpellingSuggester;
import com.infotech.lang.application.translation.TypedWord;
import com.infotech.lang.application.voice.VoiceInput;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A text box for answers in the tested language: English letters turn into Tamil/Hindi as the learner types, Meta spelling help
 * appears as buttons that replace exactly the words they cover, and a microphone button lets the learner speak (Meta speech-to-text)
 * with the words landing in the same box, editable. {@link #logPrefix(String)} tells how the answer was produced so spoken answers
 * and spelling help can be marked in the record.
 */
@JsModule("./components/transliterate.js")
public class SmartAnswerBox extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SmartAnswerBox.class);

    private final String language;
    private final transient SpellingSuggester speller;
    private final transient ObjectMapper json = new ObjectMapper();
    private final TextArea box = new TextArea();
    private final Div suggestionRow = new Div();

    private String lastSuggestedFor = "";
    private int suggestionsUsed;
    private boolean voiceUsed;
    private String voiceText = "";

    public SmartAnswerBox(String language, SpellingSuggester speller, SpeechToText speech) {
        this.language = language;
        this.speller = speller;
        setPadding(false);
        setSpacing(false);
        String name = "hindi".equalsIgnoreCase(language) ? "Hindi" : "Tamil";
        box.setPlaceholder("Type your answer here");
        box.setWidthFull();
        box.setMinHeight("90px");
        box.setMaxLength(400);
        box.getStyle().set("font-size", "1.3em");
        box.setValueChangeMode(ValueChangeMode.LAZY);
        box.setValueChangeTimeout(900);
        box.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                offerSpelling();
            }
        });
        suggestionRow.getStyle().set("min-height", "2.5em");
        Paragraph hint = new Paragraph("Tip: type " + name + " words in English letters (for example "
                + ("hindi".equalsIgnoreCase(language) ? "\"mai paanee peetaa hoon\"" : "\"naan thanneer kudikkiren\"")
                + ") and they turn into " + name + " letters. When you pause, Meta may suggest a better spelling; tap it to use it. "
                + "You can also use your own " + name + " keyboard or speak.");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        add(box, new VoiceInput(speech, language, this::takeSpokenText), suggestionRow, hint);
        addAttachListener(e -> box.getElement().executeJs("window.tmlTransliterate && window.tmlTransliterate.attach(this, $0)",
                "hindi".equalsIgnoreCase(language) ? "hindi" : "tamil"));
    }

    public String text() {
        return box.getValue() == null ? "" : box.getValue().trim();
    }

    public void clear() {
        box.clear();
        suggestionRow.removeAll();
        lastSuggestedFor = "";
        suggestionsUsed = 0;
        voiceUsed = false;
        voiceText = "";
        box.getElement().executeJs("this.__tmlSync && this.__tmlSync()");
    }

    public void setBoxEnabled(boolean enabled) {
        box.setEnabled(enabled);
    }

    /** How the answer was produced, for the record: "VOICE ", "VOICE-EDITED " and/or "SPELLING-HELP n " (empty for plain typing). */
    public String logPrefix(String given) {
        String voice = !voiceUsed ? "" : (given.trim().equals(voiceText.trim()) ? "VOICE " : "VOICE-EDITED ");
        return voice + (suggestionsUsed > 0 ? "SPELLING-HELP " + suggestionsUsed + " " : "");
    }

    private void takeSpokenText(String spoken) {
        String current = text();
        String joined = current.isEmpty() ? spoken : current + " " + spoken;
        box.setValue(joined);
        voiceUsed = true;
        voiceText = joined;
        suggestionRow.removeAll();
        box.getElement().executeJs("this.__tmlSync && this.__tmlSync()");
    }

    private void offerSpelling() {
        box.getElement().executeJs("return this.getAttribute('data-words') || '[]'").then(String.class, raw -> {
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
                showRuns(new ArrayList<>(SpellingRun.group(typed, speller.suggestWords(language, typed))));
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
            all.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            suggestionRow.add(all);
        }
    }

    private void applyRun(SpellingRun run, List<SpellingRun> runs) {
        box.getElement().executeJs("return this.__tmlReplace ? this.__tmlReplace($0, $1, $2, $3) : false",
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
                        lastSuggestedFor = "";
                        offerSpelling();
                    }
                });
    }

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
        box.getElement().executeJs(script.toString()).then(Boolean.class, done -> {
            suggestionsUsed += sorted.size();
            suggestionRow.removeAll();
            lastSuggestedFor = "";
            if (!Boolean.TRUE.equals(done)) {
                offerSpelling();
            }
        });
    }
}
