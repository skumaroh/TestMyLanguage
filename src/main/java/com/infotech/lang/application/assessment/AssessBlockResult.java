package com.infotech.lang.application.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** The result of one block (for example the words block) inside an attempt. */
@Entity
@Table(name = "assess_block_result")
public class AssessBlockResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "block_code", nullable = false, length = 30)
    private String blockCode;

    @Column(name = "block_order", nullable = false)
    private int blockOrder;

    @Column(name = "asked", nullable = false)
    private int asked;

    @Column(name = "correct", nullable = false)
    private int correct;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public void setBlockCode(String blockCode) {
        this.blockCode = blockCode;
    }

    public void setBlockOrder(int blockOrder) {
        this.blockOrder = blockOrder;
    }

    public void setAsked(int asked) {
        this.asked = asked;
    }

    public void setCorrect(int correct) {
        this.correct = correct;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
