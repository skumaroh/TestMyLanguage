package com.infotech.lang.application.views;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * Shows whether the answer just given was correct or wrong, for a second and a half, to everyone: students, guests and admins. Students
 * and guests see only "Correct" or "Wrong"; admins testing a block also see the detail (the expected answer, the reason Meta gave).
 */
final class AnswerFeedback {

    static final int SHOWN_MILLIS = 1500;

    private AnswerFeedback() {
    }

    static void show(boolean correct, String detail) {
        boolean detailed = MainLayout.isAdmin() && detail != null && !detail.isBlank();
        String text = (correct ? "Correct" : "Wrong") + (detailed ? " – " + detail : "");
        Notification n = Notification.show(text, SHOWN_MILLIS, Notification.Position.TOP_CENTER);
        n.addThemeVariants(correct ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR);
    }
}
