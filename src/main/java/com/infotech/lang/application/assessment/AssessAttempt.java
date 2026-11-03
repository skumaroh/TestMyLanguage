package com.infotech.lang.application.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** One attempt at a level by one student. */
@Entity
@Table(name = "assess_attempt")
public class AssessAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "total_asked", nullable = false)
    private int totalAsked;

    @Column(name = "total_correct", nullable = false)
    private int totalCorrect;

    @Column(name = "percent", precision = 5, scale = 2)
    private java.math.BigDecimal percent;

    @Column(name = "level_passed")
    private Boolean levelPassed;

    public UUID getUserId() {
        return userId;
    }

    public String getLanguage() {
        return language;
    }

    public int getLevel() {
        return level;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public int getTotalAsked() {
        return totalAsked;
    }

    public int getTotalCorrect() {
        return totalCorrect;
    }

    public java.math.BigDecimal getPercent() {
        return percent;
    }

    public Boolean getLevelPassed() {
        return levelPassed;
    }

    /** Fixes the totals and the level result when the attempt is completed. */
    public void complete(int totalAsked, int totalCorrect, java.math.BigDecimal percent, Boolean levelPassed) {
        this.totalAsked = totalAsked;
        this.totalCorrect = totalCorrect;
        this.percent = percent;
        this.levelPassed = levelPassed;
        this.status = "COMPLETED";
        this.completedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
