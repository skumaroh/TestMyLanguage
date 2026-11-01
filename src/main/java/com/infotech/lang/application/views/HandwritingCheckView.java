package com.infotech.lang.application.views;

import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.handwriting.CombinedHandwritingCheck;
import com.infotech.lang.application.handwriting.DrawingPad;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Locale;

/**
 * Admin test page for the handwriting check: draw one character, type the letter you meant, and see what the Meta vision
 * model reads and how confident it is. Nothing is saved except the model-call log row (no picture, no answer).
 */
@Route("admin/handwriting-check")
@PageTitle("Handwriting check | TestMyLanguage")
@RolesAllowed("ADMIN")
public class HandwritingCheckView extends VerticalLayout {

    private final CombinedHandwritingCheck check;
    private final String language;
    private final TextField expected = new TextField("The letter you meant to draw");
    private final Paragraph result = new Paragraph("Draw one character, then press Check with Meta.");

    public HandwritingCheckView(CombinedHandwritingCheck check, LanguageContext languageContext) {
        this.check = check;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");

        DrawingPad pad = new DrawingPad();
        pad.addDrawingExportedListener(e -> run(e.png(), e.isBlank()));

        Button clear = new Button("Clear", e -> pad.clearPad());
        Button checkButton = new Button("Check", e -> pad.exportDrawing());
        add(new H2("Handwriting check (" + language + ")"),
                new Paragraph("Two checks run: the old app's shape comparison against the letter's reference image, and "
                        + "the Meta vision model, which reads the drawing blind (it is not told which letter you meant). "
                        + "Nothing is stored except a model-call log row."),
                expected, pad, new HorizontalLayout(clear, checkButton), result);
    }

    private void run(byte[] png, boolean blank) {
        String meant = expected.getValue() == null ? "" : expected.getValue().trim();
        if (blank || png == null) {
            Notification.show("Draw a character first.");
            return;
        }
        if (meant.isEmpty()) {
            Notification.show("Type the letter you meant to draw.");
            return;
        }
        CombinedHandwritingCheck.Result r = check.check(png, language, meant);
        String local = r.localScore() == null ? "none (no reference image)" : String.format(Locale.ROOT, "%.2f", r.localScore());
        String meta = r.metaAnswered()
                ? String.format(Locale.ROOT, "read \"%s\", confidence %.2f, counted %.2f", r.metaRead(), r.metaConfidence(), r.metaScore())
                : "no answer";
        String combined = r.combined() == null ? "none" : String.format(Locale.ROOT, "%.2f", r.combined());
        result.setText("Old shape comparison: " + local + ". Meta: " + meta + " (" + r.metaMs() + " ms). Combined (old "
                + "weighs more): " + combined + ". " + r.note());
    }
}
