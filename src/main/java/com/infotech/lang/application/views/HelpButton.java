package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.Modules;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.infotech.lang.application.voice.SpeechRegistry;

/** A small "?" Help link that opens the instructions in a popup with a Close link at the top. */
final class HelpButton {

    private HelpButton() {
    }

    /** The help link; the popup shows {@code content} under the given title. */
    static Button link(String title, java.util.function.Supplier<Component> content) {
        Button help = new Button("Help", new Icon(VaadinIcon.QUESTION_CIRCLE_O));
        help.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        help.getElement().setAttribute("aria-label", "Help: " + title);
        help.addClickListener(e -> {
            Dialog dialog = new Dialog();
            dialog.setModal(true);
            dialog.setCloseOnEsc(true);
            dialog.setCloseOnOutsideClick(true);
            dialog.setMaxWidth("640px");
            dialog.setWidth("92vw");
            Button close = new Button("Close", c -> dialog.close());
            close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            H2 heading = new H2(title);
            heading.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-l)");
            HorizontalLayout top = new HorizontalLayout(heading, close);
            top.setWidthFull();
            top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            top.setAlignItems(FlexComponent.Alignment.CENTER);
            dialog.getHeader().add(top);
            dialog.add(content.get());
            dialog.open();
        });
        return help;
    }

    /** The help link whose popup is a few plain paragraphs. */
    static Button text(String title, String... paragraphs) {
        return link(title, () -> {
            com.vaadin.flow.component.html.Div body = new com.vaadin.flow.component.html.Div();
            SpeechRegistry registry = registry();
            for (String p : paragraphs) {
                Paragraph text = new Paragraph(p);
                if (speakable(p)) {
                    text.getStyle().set("display", "inline");
                    HorizontalLayout row = new HorizontalLayout(text, new SpeakButton(p.replace("\"", ""), "english", registry));
                    row.setAlignItems(FlexComponent.Alignment.CENTER);
                    row.getStyle().set("flex-wrap", "wrap");
                    body.add(row);
                } else {
                    body.add(text);
                }
            }
            return body;
        });
    }

    /** Plain English instructions can be read aloud; lines with other scripts or typing examples (spelled out in English letters) cannot. */
    static boolean speakable(String paragraph) {
        if (paragraph == null || paragraph.isBlank() || paragraph.startsWith("Tip for typing")) {
            return false;
        }
        return paragraph.chars().allMatch(c -> c < 128);
    }

    /** The speech registry, found through Spring (this helper is not itself a Spring bean); null when it cannot be found. */
    private static SpeechRegistry registry() {
        try {
            var servlet = com.vaadin.flow.server.VaadinServlet.getCurrent();
            var context = servlet == null ? null
                    : org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(servlet.getServletContext());
            return context == null ? null : context.getBean(SpeechRegistry.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** A module's heading ("Module 2 – Writing the letters") with its Help link beside it. */
    static HorizontalLayout heading(Modules module, String... instructions) {
        H2 title = new H2(module.heading());
        title.getStyle().set("margin", "0");
        HorizontalLayout row = new HorizontalLayout(title, text("How this module works", instructions));
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setWrap(true);
        return row;
    }
}
