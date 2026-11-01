package com.infotech.lang.application.views;

import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.handwriting.DrawingPad;
import com.infotech.lang.application.handwriting.HandwritingReader;
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

    private final HandwritingReader reader;
    private final String language;
    private final TextField expected = new TextField("The letter you meant to draw");
    private final Paragraph result = new Paragraph("Draw one character, then press Check with Meta.");

    public HandwritingCheckView(HandwritingReader reader, LanguageContext languageContext) {
        this.reader = reader;
        this.language = languageContext.getLanguage();
        setAlignItems(Alignment.CENTER);
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");

        DrawingPad pad = new DrawingPad();
        pad.addDrawingExportedListener(e -> check(e.png(), e.isBlank()));

        Button clear = new Button("Clear", e -> pad.clearPad());
        Button check = new Button("Check with Meta", e -> pad.exportDrawing());
        add(new H2("Handwriting check (" + language + ")"),
                new Paragraph("Draws are sent to the Meta vision model, which reads the character blind (it is not told "
                        + "which letter you meant). Nothing is stored except a model-call log row."),
                expected, pad, new HorizontalLayout(clear, check), result);
    }

    private void check(byte[] png, boolean blank) {
        if (blank || png == null) {
            Notification.show("Draw a character first.");
            return;
        }
        HandwritingReader.Reading reading = reader.read(png, language);
        if (!reading.ok()) {
            result.setText("The model did not answer (" + reading.error() + ", " + reading.latencyMs() + " ms).");
            return;
        }
        String meant = expected.getValue() == null ? "" : expected.getValue().trim();
        String same = meant.isEmpty() ? "no letter typed to compare"
                : (meant.equals(reading.character()) ? "same as the letter you meant" : "different from the letter you meant");
        result.setText(String.format(Locale.ROOT, "Meta read: \"%s\", confidence %.2f (%s). %d ms.",
                reading.character(), reading.confidence(), same, reading.latencyMs()));
    }
}
