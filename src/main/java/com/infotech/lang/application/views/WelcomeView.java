package com.infotech.lang.application.views;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.proctor.ProctorActivity;
import com.infotech.lang.application.proctor.ProctorSlot;
import com.infotech.lang.application.proctor.ProctorTextService;
import com.infotech.lang.application.proctor.ProctorWarmUp;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Welcome and activity introductions. The words are Meta-written texts an admin has approved (or a plain built-in
 * text until then). The "Listen" button uses the browser's own voice, which is not a Meta product.
 */
@Route("welcome")
@PageTitle("Welcome | TestMyLanguage")
@PermitAll
public class WelcomeView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final ProctorTextService textService;
    private final ProctorWarmUp warmUp;
    private final String testedLanguage;

    public WelcomeView(SecurityService securityService,
                       UserRepository userRepository,
                       ProctorTextService textService,
                       ProctorWarmUp warmUp,
                       @Value("${proctor.level:1}") int level) {
        this.textService = textService;
        this.warmUp = warmUp;
        this.testedLanguage = textService.testedLanguage();
        setAlignItems(Alignment.CENTER);
        getStyle().set("padding-top", "var(--lumo-space-xl)");
        setMaxWidth("720px");
        getStyle().set("margin", "0 auto");

        String name = greetingNameOf(securityService, userRepository);
        String proctorLanguage = textService.proctorLanguage();

        add(new H2("Welcome"));
        add(card(null, ProctorSlot.greeting(level, proctorLanguage), name));
        for (ProctorActivity activity : ProctorActivity.values()) {
            if (activity != ProctorActivity.NONE) {
                add(card(activity.getLabel(), ProctorSlot.intro(activity, level, proctorLanguage), name));
            }
        }
        Span note = new Span("Texts written by Meta Muse Spark and approved by an administrator. "
                + "The Listen button uses your browser's voice (not Meta).");
        note.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        add(note);
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
        for (String line : resolved.text().split("\n")) {
            card.add(new Paragraph(line));
        }
        Span source = new Span(resolved.fromMeta() ? "Written by Meta Muse Spark" : "Standard text");
        source.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)")
                .set("margin-right", "var(--lumo-space-m)");
        Button listen = new Button("Listen (browser voice, not Meta)");
        Span voiceNote = new Span();
        voiceNote.getStyle().set("display", "block").set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)").set("margin-top", "var(--lumo-space-xs)");
        attachSpeech(listen, voiceNote, resolved.text());
        card.add(source, listen, voiceNote);
        return card;
    }

    /**
     * The click is handled in the browser, not on the server: speech must start inside the click itself
     * (some browsers refuse it after a server round trip), and it keeps working if the live connection is slow.
     */
    private void attachSpeech(Button listen, Span voiceNote, String text) {
        // Only plain strings are passed to the browser (Vaadin cannot encode Java arrays); the browser splits the text.
        listen.getElement().executeJs(
                "const parts = $0.split('\\n'), testedTag = $1, note = $2, label = $3; const synth = window.speechSynthesis;"
                        // Safari reads English too fast; slow only the English part, and only there
                        + "const slowEnglish = /^((?!chrome|android|crios|fxios|edg).)*safari/i.test(navigator.userAgent);"
                        + "if (!synth) { note.textContent = 'This browser cannot read text aloud.'; return; }"
                        // asking for the list early makes the browser start loading its voices
                        + "synth.getVoices();"
                        + "this.addEventListener('click', () => {"
                        + " const voices = synth.getVoices(); let missing = false;"
                        + " synth.cancel(); synth.resume();"
                        + " parts.forEach((t, i) => { const tag = i === 0 ? 'en-US' : testedTag;"
                        + "  const u = new SpeechSynthesisUtterance(t); u.lang = tag; if (slowEnglish && i === 0) { u.rate = 0.8; }"
                        + "  const lang = tag.toLowerCase(); const base = lang.split('-')[0];"
                        + "  const v = voices.find(x => x.lang.toLowerCase().replace('_', '-') === lang)"
                        + "   || voices.find(x => x.lang.toLowerCase().replace('_', '-').startsWith(base));"
                        + "  if (v) { u.voice = v; } else if (i > 0) { missing = true; }"
                        + "  synth.speak(u); });"
                        + " note.textContent = missing ? 'This device has no ' + label + ' voice installed, so only the English part is read. '"
                        + "  + 'Add one in the device speech or language settings.' : '';"
                        + "});",
                text, speechTag(testedLanguage), voiceNote.getElement(), testedLanguage);
    }

    private static String speechTag(String testedLanguage) {
        if ("hindi".equalsIgnoreCase(testedLanguage)) {
            return "hi-IN";
        }
        if ("tamil".equalsIgnoreCase(testedLanguage)) {
            return "ta-IN";
        }
        return "en-US";
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
                .map(WelcomeView::preferredName)
                .orElse("");
    }
}
