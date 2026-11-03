package com.infotech.lang.application.views;

import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.translation.TranslationGrader;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Locale;

/**
 * Admin test page for the translation grader: type an English sentence, a reference translation (optional) and a learner's
 * answer, and see the verdict and reason. Nothing is stored except the model-call log row (no text).
 */
@Route(value = "admin/translation-check", layout = MainLayout.class)
@PageTitle("Translation check | TestMyLanguage")
@RolesAllowed("ADMIN")
public class TranslationCheckView extends VerticalLayout {

    private final TranslationGrader grader;
    private final String language;
    private final TextArea english = new TextArea("English sentence");
    private final TextArea reference = new TextArea("A correct translation (optional)");
    private final TextArea answer = new TextArea("The learner's answer");
    private final Paragraph result = new Paragraph("Fill in the boxes and press Grade.");

    public TranslationCheckView(TranslationGrader grader, LanguageContext languageContext) {
        this.grader = grader;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");
        for (TextArea box : new TextArea[] {english, reference, answer}) {
            box.setWidthFull();
            box.setMinHeight("80px");
        }
        Button grade = new Button("Grade", e -> run());
        add(new H2("Translation check (" + language + ")"),
                new Paragraph("An exact match counts without asking Meta. Anything else is judged for meaning by the Meta model, "
                        + "so a correct paraphrase should pass. Nothing is stored except a model-call log row."),
                english, reference, answer, grade, result);
    }

    private void run() {
        if (english.getValue().isBlank() || answer.getValue().isBlank()) {
            Notification.show("Fill in the English sentence and the learner's answer.");
            return;
        }
        TranslationGrader.Grade g = grader.grade(language, english.getValue(), reference.getValue(), answer.getValue());
        result.setText(String.format(Locale.ROOT, "%s: %s%s. %s", g.correct() ? "CORRECT" : "NOT CORRECT", g.verdict(),
                g.usedModel() ? " (asked Meta, " + g.latencyMs() + " ms)" : " (no model call)",
                g.reason().isBlank() ? "" : "Reason: " + g.reason()));
    }
}
