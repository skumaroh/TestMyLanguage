package com.infotech.lang.application.views;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.proctor.ProctorActivity;
import com.infotech.lang.application.proctor.ProctorSlot;
import com.infotech.lang.application.proctor.ProctorTextService;
import com.infotech.lang.application.proctor.ProctorWarmUp;
import com.infotech.lang.application.security.SecurityService;
import com.infotech.lang.application.voice.SpeechRegistry;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The welcome and activity introductions, shown in the home page's help popup. The words are Meta-written texts an admin has approved (or a plain built-in
 * text until then). Every line has a "Listen" button that plays Meta's open-source MMS voice from our speech service (English for the English
 * line, the tested language for the lines after it), falling back to the device's own voice.
 */
public class WelcomeIntro extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final ProctorTextService textService;
    private final ProctorWarmUp warmUp;
    private final String testedLanguage;
    private final SpeechRegistry speech;

    public WelcomeIntro(SecurityService securityService,
                       UserRepository userRepository,
                       ProctorTextService textService,
                       ProctorWarmUp warmUp,
                       SpeechRegistry speech,
                       int level) {
        this.speech = speech;
        this.textService = textService;
        this.warmUp = warmUp;
        this.testedLanguage = textService.testedLanguage();
        setAlignItems(Alignment.CENTER);
        setPadding(false);

        String name = greetingNameOf(securityService, userRepository);
        String proctorLanguage = textService.proctorLanguage();

        add(card(null, ProctorSlot.greeting(level, proctorLanguage), name));
        for (ProctorActivity activity : ProctorActivity.values()) {
            if (activity != ProctorActivity.NONE) {
                add(card(activity.getLabel(), ProctorSlot.intro(activity, level, proctorLanguage), name));
            }
        }
        Span note = new Span("Texts written by Meta Muse Spark and approved by an administrator. "
                + "The Listen buttons play Meta's open-source MMS voice (your device's own voice if that is not available).");
        note.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        add(note);
        add(new H3("What each module looks like"), HelpImages.gallery());
    }

    private Div card(String heading, ProctorSlot slot, String name) {
        ProctorTextService.ResolvedText resolved = textService.resolve(slot, name);
        if (!resolved.fromMeta()) {
            warmUp.requestGenerationIfNeeded(slot);
        }
        Div card = new Div();
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("width", "100%").set("box-sizing", "border-box");
        if (heading != null) {
            card.add(new H3(heading));
        }
        String[] lines = resolved.text().split("\n");
        boolean englishFirst = "en".equalsIgnoreCase(textService.proctorLanguage());
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String voice = englishFirst && i == 0 ? "english" : testedLanguage;
            Paragraph text = new Paragraph(lines[i]);
            text.getStyle().set("margin", "0");
            HorizontalLayout row = new HorizontalLayout(text, new SpeakButton(lines[i], voice, speech));
            row.setAlignItems(Alignment.CENTER);
            row.getStyle().set("flex-wrap", "wrap").set("margin-bottom", "var(--lumo-space-xs)");
            card.add(row);
        }
        Span source = new Span(resolved.fromMeta() ? "Written by Meta Muse Spark" : "Standard text");
        source.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)")
                .set("margin-right", "var(--lumo-space-m)");
        card.add(source);
        return card;
    }

    /** The name the student chose to be called (display name), then the first name; never the short login id. */
    private static String preferredName(User user) {
        String display = user.getDisplayName();
        if (display != null && !display.isBlank()) {
            return display.trim();
        }
        String first = user.getFirstName();
        return first == null ? "" : first.trim();
    }

    private static String greetingNameOf(SecurityService securityService, UserRepository userRepository) {
        UserDetails user = securityService.getAuthenticatedUser();
        if (user == null) {
            return "";
        }
        return userRepository.findByEmailIgnoreCase(user.getUsername())
                .map(WelcomeIntro::preferredName)
                .orElse("");
    }
}
