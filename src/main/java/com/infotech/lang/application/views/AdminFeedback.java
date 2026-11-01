package com.infotech.lang.application.views;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * Shows an admin whether the answer just given was correct or wrong, with a short detail. Students never see it
 * (correctness stays hidden until the final report); only signed-in admins testing a block do.
 */
final class AdminFeedback {

    private AdminFeedback() {
    }

    static void show(boolean correct, String detail) {
        if (!MainLayout.isAdmin()) {
            return;
        }
        String text = (correct ? "Correct" : "Wrong") + (detail == null || detail.isBlank() ? "" : " – " + detail);
        Notification n = Notification.show(text, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(correct ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
    }
}
