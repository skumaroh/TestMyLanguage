package com.infotech.lang.application.views;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.proctor.ProctorActivity;
import com.infotech.lang.application.proctor.ProctorSlot;
import com.infotech.lang.application.proctor.ProctorTextService;
import com.infotech.lang.application.proctor.ProctorWarmUp;
import com.infotech.lang.application.security.SecurityService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
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

        String firstName = firstNameOf(securityService, userRepository);
        String proctorLanguage = textService.proctorLanguage();

        add(new H2("Welcome"));
        add(card(null, ProctorSlot.greeting(level, proctorLanguage), firstName));
        for (ProctorActivity activity : ProctorActivity.values()) {
            if (activity != ProctorActivity.NONE) {
                add(card(activity.getLabel(), ProctorSlot.intro(activity, level, proctorLanguage), firstName));
            }
        }
        Span note = new Span("Texts written by Meta Muse Spark and approved by an administrator. "
                + "The Listen button uses your browser's voice (not Meta).");
        note.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        add(note);
    }

    private Div card(String heading, ProctorSlot slot, String firstName) {
        ProctorTextService.ResolvedText resolved = textService.resolve(slot, firstName);
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
        Button listen = new Button("Listen (browser voice, not Meta)", e -> speak(resolved.text()));
        card.add(source, listen);
        return card;
    }

    private void speak(String text) {
        String[] parts = text.split("\n");
        String[] languages = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            languages[i] = i == 0 ? "en-US" : speechTag(testedLanguage);
        }
        UI.getCurrent().getPage().executeJs(
                "if (!('speechSynthesis' in window)) { return 'unsupported'; }"
                        + "const parts = $0; const langs = $1; const synth = window.speechSynthesis;"
                        // Browsers load their voice list late; wait briefly for it before choosing voices.
                        + "const voicesReady = new Promise(resolve => {"
                        + " if (synth.getVoices().length > 0) { resolve(); return; }"
                        + " synth.addEventListener('voiceschanged', () => resolve(), { once: true });"
                        + " setTimeout(resolve, 1500); });"
                        + "return voicesReady.then(() => new Promise(done => {"
                        + " const voices = synth.getVoices(); let missing = false;"
                        + " synth.cancel(); synth.resume();"
                        // a short pause after cancel() stops Chrome from dropping the first utterance
                        + " setTimeout(() => {"
                        + "  parts.forEach((t, i) => { const u = new SpeechSynthesisUtterance(t); u.lang = langs[i];"
                        + "   const lang = langs[i].toLowerCase(); const base = lang.split('-')[0];"
                        + "   const v = voices.find(x => x.lang.toLowerCase().replace('_', '-') === lang)"
                        + "     || voices.find(x => x.lang.toLowerCase().replace('_', '-').startsWith(base));"
                        + "   if (v) { u.voice = v; } else if (i > 0) { missing = true; }"
                        + "   synth.speak(u); });"
                        + "  done(missing ? 'novoice' : 'ok'); }, 60); }));", parts, languages)
                .then(String.class, result -> {
                    if ("unsupported".equals(result)) {
                        Notification.show("Your browser cannot read text aloud.");
                    } else if ("novoice".equals(result)) {
                        Notification.show("This device has no " + testedLanguage
                                + " voice installed, so only the English part can be read. "
                                + "Add one in the device's speech or language settings.", 8000,
                                Notification.Position.MIDDLE);
                    }
                });
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

    private static String firstNameOf(SecurityService securityService, UserRepository userRepository) {
        UserDetails user = securityService.getAuthenticatedUser();
        if (user == null) {
            return "";
        }
        return userRepository.findByEmailIgnoreCase(user.getUsername())
                .map(User::getFirstName)
                .orElse("");
    }
}
