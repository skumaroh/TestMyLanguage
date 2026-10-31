package com.infotech.lang.application.proctor;

/** Plain built-in texts shown until an admin has approved a Meta-written one (or if anything fails). English only. */
public final class ProctorFallbacks {

    private ProctorFallbacks() {
    }

    public static String forSlot(ProctorSlot slot) {
        if (slot.purpose() == ProctorPurpose.GREETING) {
            return "Welcome, {name}! In this placement assessment you will try five short activities. Take your time and do your best.";
        }
        return switch (slot.activity()) {
            case RECOGNITION -> "In this activity you will look at pictures, words and letters and show what you recognise.";
            case HANDWRITING -> "In this activity you will write letters by hand on the screen.";
            case TRANSLATION -> "In this activity you will translate short sentences from English.";
            case PERSONA -> "In this activity you will have a short conversation with a friendly character.";
            case VIDEO -> "In this activity you will watch a short video and choose what it shows.";
            default -> "Get ready for the next activity.";
        };
    }
}
