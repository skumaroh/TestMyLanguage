package com.infotech.lang.application.video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * One short action clip (or picture) of the library. The learner watches it and says what is happening in the tested language.
 * Only {@link #VERIFIED} clips are used in a test; clips are retired, never deleted.
 */
@Entity
@Table(name = "assess_video_clip")
public class VideoClip {

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

    @Column(name = "kind", nullable = false, length = 10)
    private String kind = "VIDEO";

    @Column(name = "blob_name", nullable = false, length = 300)
    private String blobName;

    @Column(name = "status", nullable = false, length = 20)
    private String status = DRAFT;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "MUSE_CHAT";

    @Column(name = "english", columnDefinition = "text")
    private String english;

    @Column(name = "expected_text", columnDefinition = "text")
    private String expectedText;

    @Column(name = "variants", columnDefinition = "text")
    private String variants;

    @Column(name = "difficulty", nullable = false, length = 10)
    private String difficulty = "EASY";

    @Column(name = "meta_clear", length = 5)
    private String metaClear;

    @Column(name = "meta_problems", columnDefinition = "text")
    private String metaProblems;

    @Column(name = "model", length = 60)
    private String model;

    @Column(name = "prompt_version")
    private Integer promptVersion;

    @Column(name = "size_bytes")
    private Long sizeBytes;

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

    protected VideoClip() {
        // for JPA
    }

    public VideoClip(String language, int level, String blobName, String kind, Long sizeBytes) {
        this.language = language;
        this.level = level;
        this.blobName = blobName;
        this.kind = kind;
        this.sizeBytes = sizeBytes;
    }

    public Long getId() { return id; }
    public String getLanguage() { return language; }
    public int getLevel() { return level; }
    public String getKind() { return kind; }
    public String getBlobName() { return blobName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEnglish() { return english; }
    public void setEnglish(String english) { this.english = english; }
    public String getExpectedText() { return expectedText; }
    public void setExpectedText(String expectedText) { this.expectedText = expectedText; }
    public String getVariants() { return variants; }
    public void setVariants(String variants) { this.variants = variants; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getMetaClear() { return metaClear; }
    public void setMetaClear(String metaClear) { this.metaClear = metaClear; }
    public String getMetaProblems() { return metaProblems; }
    public void setMetaProblems(String metaProblems) { this.metaProblems = metaProblems; }
    public void setModel(String model) { this.model = model; }
    public void setPromptVersion(Integer promptVersion) { this.promptVersion = promptVersion; }
    public Long getSizeBytes() { return sizeBytes; }
    public int getTimesUsed() { return timesUsed; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }

    public boolean isVideo() {
        return "VIDEO".equals(kind);
    }

    /** True once Meta (or an admin) has filled in the description and the expected answer. */
    public boolean hasAnswer() {
        return english != null && !english.isBlank() && expectedText != null && !expectedText.isBlank();
    }

    /** The canonical answer followed by the other accepted forms. */
    public List<String> acceptedAnswers() {
        java.util.ArrayList<String> all = new java.util.ArrayList<>();
        if (expectedText != null && !expectedText.isBlank()) {
            all.add(expectedText.trim());
        }
        if (variants != null) {
            Arrays.stream(variants.split(";")).map(String::trim).filter(s -> !s.isEmpty()).forEach(all::add);
        }
        return all;
    }

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
