package com.infotech.lang.application.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * A "Listen" button that reads a line aloud with the browser's own voice (the Meta model returns text only). The click is handled in
 * the browser so speech starts inside the click. Where the device has no voice for the language the button says so.
 */
@JsModule("./components/speak.js")
public class SpeakButton extends Button {

    private static final long serialVersionUID = 1L;

    public SpeakButton(String text, String language) {
        super("▶ Listen");
        addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        getElement().setAttribute("aria-label", "Listen to this line");
        String tag = "hindi".equalsIgnoreCase(language) ? "hi-IN" : "ta-IN";
        addAttachListener(e -> getElement().executeJs("window.tmlSpeak && window.tmlSpeak.attach(this, $0, $1)", text, tag));
    }
}
