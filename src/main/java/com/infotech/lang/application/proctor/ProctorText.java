package com.infotech.lang.application.proctor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A stored proctor text. {@code body} is the English part, a newline, then a short phrase in the tested language.
 * It may contain the placeholder {@code {name}}, which our own code fills in; no student data goes to the model.
 */
@Entity
@Table(name = "assess_proctor_text")
public class ProctorText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private ProctorPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_code", nullable = false, length = 30)
    private ProctorActivity activity = ProctorActivity.NONE;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "proctor_language", nullable = false, length = 10)
    private String proctorLanguage;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "model", nullable = false, length = 60)
    private String model;

    @Column(name = "prompt_version", nullable = false)
    private int promptVersion = 1;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "META";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProctorStatus status = ProctorStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected ProctorText() {
        // for JPA
    }

    public ProctorText(ProctorSlot slot, String body, String model, int promptVersion) {
        this.purpose = slot.purpose();
        this.activity = slot.activity();
        this.level = slot.level();
        this.proctorLanguage = slot.proctorLanguage();
        this.body = body;
        this.model = model;
        this.promptVersion = promptVersion;
    }

    public ProctorSlot slot() {
        return new ProctorSlot(purpose, activity, level, proctorLanguage);
    }

    public Long getId() {
        return id;
    }

    public ProctorPurpose getPurpose() {
        return purpose;
    }

    public ProctorActivity getActivity() {
        return activity;
    }

    public int getLevel() {
        return level;
    }

    public String getProctorLanguage() {
        return proctorLanguage;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getModel() {
        return model;
    }

    public int getPromptVersion() {
        return promptVersion;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ProctorStatus getStatus() {
        return status;
    }

    public void setStatus(ProctorStatus status) {
        this.status = status;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
