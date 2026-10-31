package com.infotech.lang.application.proctor;

/** The five activities of an assessment level, plus NONE for texts that belong to no activity (the greeting). */
public enum ProctorActivity {
    NONE("", ""),
    RECOGNITION("Picture, word and letter recognition", "recognising pictures, words and letters"),
    HANDWRITING("Handwriting", "writing letters by hand on the screen"),
    TRANSLATION("Sentence translation", "translating short sentences from English"),
    PERSONA("Conversation", "having a short conversation with a friendly character"),
    VIDEO("Video comprehension", "watching a short video and choosing what it shows");

    private final String label;
    private final String description;

    ProctorActivity(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
