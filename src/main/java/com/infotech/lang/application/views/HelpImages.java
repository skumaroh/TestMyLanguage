package com.infotech.lang.application.views;

import com.infotech.lang.application.assessment.Modules;
import com.infotech.lang.application.config.LanguageContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import org.springframework.core.io.ClassPathResource;

/**
 * Sample screenshots for the Help popups, one per module and language, kept in
 * {@code META-INF/resources/images/help/<language>/module<N>.jpg}. A module without a screenshot for the language simply shows none.
 */
final class HelpImages {

    private HelpImages() {
    }

    /** The module's sample screenshot for the deployed language, or null when there is none. */
    static Component forModule(Modules module) {
        return forModule(module, language());
    }

    static Component forModule(Modules module, String language) {
        String path = path(module, language);
        if (path == null || !new ClassPathResource("META-INF/resources/" + path).exists()) {
            return null;
        }
        Image image = new Image(path, "Sample screen of " + module.heading());
        image.getStyle().set("max-width", "100%").set("height", "auto").set("display", "block").set("margin", "0 auto")
                .set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "var(--lumo-border-radius-m)");
        return image;
    }

    /** Every module's screenshot with its heading above it (modules without one are left out); used on the first page's help. */
    static Component gallery() {
        Div all = new Div();
        for (Modules module : Modules.values()) {
            Component image = forModule(module);
            if (image != null) {
                H3 title = new H3(module.heading());
                title.getStyle().set("margin", "var(--lumo-space-m) 0 var(--lumo-space-xs)");
                all.add(title, image);
            }
        }
        return all;
    }

    static String path(Modules module, String language) {
        if (language == null || !language.matches("[a-z]+")) {
            return null;
        }
        return "images/help/" + language + "/module" + module.number() + ".jpg";
    }

    private static String language() {
        try {
            var servlet = com.vaadin.flow.server.VaadinServlet.getCurrent();
            var context = servlet == null ? null
                    : org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(servlet.getServletContext());
            return context == null ? null : context.getBean(LanguageContext.class).getLanguage();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
