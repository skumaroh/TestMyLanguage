package com.infotech.lang.application.views;

import com.infotech.lang.application.ai.VoiceStyle;
import com.infotech.lang.application.config.LanguageContext;
import com.infotech.lang.application.voice.SpeechRegistry;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Admin-only voice lab: hear one sample line in every persona voice preset side by side, or with your own pitch and speed, to tune the
 * numbers by ear. The Meta MMS voices are single-speaker (both are male today); pitch and speed are applied to the audio by the speech
 * service. Admin only; nothing here is stored.
 */
@Route(value = "admin/voice-lab", layout = MainLayout.class)
@PageTitle("Voice lab | TestMyLanguage")
@RolesAllowed("ADMIN")
public class VoiceLabView extends VerticalLayout {

    private static final long serialVersionUID = 1L;
    private static final int MAX_SAMPLE_CHARS = 200;

    private final String language;
    private final transient SpeechRegistry registry;
    private final TextField sample = new TextField("Sample line (" + "in the tested language" + ")");
    private final NumberField pitch = new NumberField("Pitch (semitones, + higher)");
    private final NumberField rate = new NumberField("Speed (1 = normal)");
    private final HorizontalLayout custom = new HorizontalLayout();
    private final Grid<VoiceStyle.Preset> grid = new Grid<>(VoiceStyle.Preset.class, false);

    public VoiceLabView(LanguageContext languageContext, SpeechRegistry registry) {
        this.language = languageContext.getLanguage();
        this.registry = registry;
        add(new H2("Voice lab (" + language + ")"));
        add(new Paragraph("The Meta voice for this language is one fixed voice, and it sounds male today. Pitch and speed are changed on the audio "
                + "afterwards, so small steps sound natural and large ones robotic (limits: pitch ±8, speed 0.7 to 1.3). Press Listen on a row to hear a "
                + "preset, or set your own numbers. Tell me which presets sound right and what to change."));
        if (!registry.isEnabled()) {
            add(new Paragraph("The speech service is not configured here, so the Meta voice is not available and the buttons use the browser voice "
                    + "(which cannot change pitch)."));
        }

        sample.setWidthFull();
        sample.setMaxLength(MAX_SAMPLE_CHARS);
        sample.setValue("hindi".equalsIgnoreCase(language) ? "नमस्ते! आपका नाम क्या है? मुझे आपसे मिलकर खुशी हुई।"
                : "வணக்கம்! உங்கள் பெயர் என்ன? உங்களைப் பார்த்ததில் மகிழ்ச்சி.");
        sample.setValueChangeMode(ValueChangeMode.LAZY);
        sample.addValueChangeListener(e -> refresh());
        add(sample);

        add(new H3("Your own numbers"));
        for (NumberField f : new NumberField[] {pitch, rate}) {
            f.setStepButtonsVisible(true);
            f.setValueChangeMode(ValueChangeMode.LAZY);
            f.addValueChangeListener(e -> refresh());
        }
        pitch.setMin(-8);
        pitch.setMax(8);
        pitch.setStep(0.5);
        pitch.setValue(0.0);
        rate.setMin(0.7);
        rate.setMax(1.3);
        rate.setStep(0.05);
        rate.setValue(1.0);
        custom.setAlignItems(Alignment.END);
        add(custom);

        add(new H3("Presets (the starting values for persona voices)"));
        grid.addColumn(VoiceStyle.Preset::name).setHeader("Voice").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(p -> fmt(p.style().pitch())).setHeader("Pitch").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(p -> fmt(p.style().rate())).setHeader("Speed").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(p -> new SpeakButton(text(), language, registry, p.style())).setHeader("").setAutoWidth(true);
        grid.setItems(VoiceStyle.presets());
        grid.setAllRowsVisible(true);
        add(grid);
        setWidthFull();
        refresh();
    }

    /** Rebuilds the buttons so they speak the current sample and numbers. */
    private void refresh() {
        custom.removeAll();
        double p = pitch.getValue() == null ? 0.0 : pitch.getValue();
        double r = rate.getValue() == null ? 1.0 : rate.getValue();
        custom.add(pitch, rate, new SpeakButton(text(), language, registry, new VoiceStyle(p, r)));
        grid.getDataProvider().refreshAll();
    }

    private String text() {
        String t = sample.getValue() == null ? "" : sample.getValue().trim();
        return t.isEmpty() ? "." : t;
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}
