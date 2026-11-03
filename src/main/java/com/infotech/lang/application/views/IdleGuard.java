package com.infotech.lang.application.views;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;

/**
 * Ends a waiting question when the learner has been inactive: after the quiet period a countdown warning is shown; any key, tap
 * or click cancels it; at zero {@code onTimeout} runs. The timer runs in the browser, so it keeps going even if the server is slow.
 */
@JsModule("./components/idle-guard.js")
public class IdleGuard extends Div {

    private static final long serialVersionUID = 1L;

    private final transient Runnable onTimeout;

    public IdleGuard(Runnable onTimeout, int quietSeconds, int warnSeconds) {
        this.onTimeout = onTimeout;
        Div warning = new Div();
        warning.getStyle().set("display", "none").set("padding", "var(--lumo-space-s)")
                .set("background", "var(--lumo-error-color-10pct)").set("color", "var(--lumo-error-text-color)")
                .set("border-radius", "var(--lumo-border-radius-m)").set("margin", "var(--lumo-space-s) 0");
        add(warning);
        addAttachListener(e -> getElement().executeJs("window.tmlIdle && window.tmlIdle.attach(this, $0, $1, $2)",
                warning.getElement(), quietSeconds * 1000, warnSeconds));
    }

    public void start() {
        getElement().executeJs("this.__idleStart && this.__idleStart()");
    }

    public void stop() {
        getElement().executeJs("this.__idleStop && this.__idleStop()");
    }

    /** Called by the browser when the countdown reaches zero. */
    @ClientCallable
    public void idleTimeout() {
        onTimeout.run();
    }
}
