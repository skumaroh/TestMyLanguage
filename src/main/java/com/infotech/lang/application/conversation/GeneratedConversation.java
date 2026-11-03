package com.infotech.lang.application.conversation;

import java.util.List;

/** What the model wrote for one new conversation, before it is saved as a DRAFT. */
public record GeneratedConversation(String scenarioTitle, String personaName, String personaDescription, String sceneNote,
                                    String introLine, String introEnglish, String closingLine, String closingEnglish,
                                    String difficulty, List<Objective> objectives) {

    public record Objective(String objectiveEn, String vocabularyHint, String fallbackLine) {
    }
}
