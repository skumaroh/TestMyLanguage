package com.infotech.lang.application.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One imagined everyday conversation of the library (who, where, the persona's opening and closing lines in the tested
 * language). Only {@link #VERIFIED} conversations are used in a test. Learners' replies are never stored here.
 */
@Entity
@Table(name = "assess_conversation")
public class Conversation {

    public static final String DRAFT = "DRAFT";
    public static final String VERIFIED = "VERIFIED";
    public static final String RETIRED = "RETIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "status", nullable = false, length = 20)
    private String status = DRAFT;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "META";

    @Column(name = "scenario_title", nullable = false, length = 200)
    private String scenarioTitle;

    @Column(name = "persona_name", nullable = false, length = 100)
    private String personaName;

    @Column(name = "persona_description", columnDefinition = "text")
    private String personaDescription;

    @Column(name = "scene_note", columnDefinition = "text")
    private String sceneNote;

    @Column(name = "intro_line", nullable = false, columnDefinition = "text")
    private String introLine;

    @Column(name = "intro_english", columnDefinition = "text")
    private String introEnglish;

    @Column(name = "closing_line", nullable = false, columnDefinition = "text")
    private String closingLine;

    @Column(name = "closing_english", columnDefinition = "text")
    private String closingEnglish;

    @Column(name = "difficulty", nullable = false, length = 10)
    private String difficulty = "EASY";

    @Column(name = "model", length = 60)
    private String model;

    @Column(name = "prompt_version")
    private Integer promptVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "times_used", nullable = false)
    private int timesUsed;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    protected Conversation() {
        // for JPA
    }

    public Conversation(String language, int level, String source, String scenarioTitle, String personaName) {
        this.language = language;
        this.level = level;
        this.source = source;
        this.scenarioTitle = scenarioTitle;
        this.personaName = personaName;
    }

    public Long getId() { return id; }
    public String getLanguage() { return language; }
    public int getLevel() { return level; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public String getScenarioTitle() { return scenarioTitle; }
    public void setScenarioTitle(String scenarioTitle) { this.scenarioTitle = scenarioTitle; }
    public String getPersonaName() { return personaName; }
    public void setPersonaName(String personaName) { this.personaName = personaName; }
    public String getPersonaDescription() { return personaDescription; }
    public void setPersonaDescription(String personaDescription) { this.personaDescription = personaDescription; }
    public String getSceneNote() { return sceneNote; }
    public void setSceneNote(String sceneNote) { this.sceneNote = sceneNote; }
    public String getIntroLine() { return introLine; }
    public void setIntroLine(String introLine) { this.introLine = introLine; }
    public String getIntroEnglish() { return introEnglish; }
    public void setIntroEnglish(String introEnglish) { this.introEnglish = introEnglish; }
    public String getClosingLine() { return closingLine; }
    public void setClosingLine(String closingLine) { this.closingLine = closingLine; }
    public String getClosingEnglish() { return closingEnglish; }
    public void setClosingEnglish(String closingEnglish) { this.closingEnglish = closingEnglish; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getPromptVersion() { return promptVersion; }
    public void setPromptVersion(Integer promptVersion) { this.promptVersion = promptVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getVerifiedBy() { return verifiedBy; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public int getTimesUsed() { return timesUsed; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }

    public void markVerified(UUID by) {
        this.status = VERIFIED;
        this.verifiedBy = by;
        this.verifiedAt = OffsetDateTime.now();
    }

    public void markUsed() {
        this.timesUsed++;
        this.lastUsedAt = OffsetDateTime.now();
    }
}
