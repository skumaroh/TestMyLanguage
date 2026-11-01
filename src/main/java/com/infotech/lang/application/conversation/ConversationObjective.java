package com.infotech.lang.application.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One objective of a conversation: what the learner must manage to say. Positions 1 to 4 are the first round, 5 to 8 the extra round. */
@Entity
@Table(name = "assess_conversation_objective")
public class ConversationObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "objective_en", nullable = false, columnDefinition = "text")
    private String objectiveEn;

    @Column(name = "vocabulary_hint", columnDefinition = "text")
    private String vocabularyHint;

    @Column(name = "fallback_line", nullable = false, columnDefinition = "text")
    private String fallbackLine;

    protected ConversationObjective() {
        // for JPA
    }

    public ConversationObjective(Long conversationId, int position, String objectiveEn, String vocabularyHint, String fallbackLine) {
        this.conversationId = conversationId;
        this.position = position;
        this.objectiveEn = objectiveEn;
        this.vocabularyHint = vocabularyHint;
        this.fallbackLine = fallbackLine;
    }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public int getPosition() { return position; }
    public String getObjectiveEn() { return objectiveEn; }
    public void setObjectiveEn(String objectiveEn) { this.objectiveEn = objectiveEn; }
    public String getVocabularyHint() { return vocabularyHint; }
    public void setVocabularyHint(String vocabularyHint) { this.vocabularyHint = vocabularyHint; }
    public String getFallbackLine() { return fallbackLine; }
    public void setFallbackLine(String fallbackLine) { this.fallbackLine = fallbackLine; }
}
