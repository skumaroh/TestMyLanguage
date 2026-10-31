package com.infotech.lang.application.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** One question answered: the item, what the student gave, and 1 or 0 as correct or not. */
@Entity
@Table(name = "assess_answer")
public class AssessAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_result_id", nullable = false)
    private Long blockResultId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "item_ref", nullable = false, length = 60)
    private String itemRef;

    @Column(name = "answer_given", columnDefinition = "text")
    private String answerGiven;

    @Column(name = "correct", nullable = false)
    private boolean correct;

    @Column(name = "answered_at", nullable = false)
    private OffsetDateTime answeredAt = OffsetDateTime.now();

    @Column(name = "duration_ms")
    private Integer durationMs;

    public void setBlockResultId(Long blockResultId) {
        this.blockResultId = blockResultId;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setItemRef(String itemRef) {
        this.itemRef = itemRef;
    }

    public void setAnswerGiven(String answerGiven) {
        this.answerGiven = answerGiven;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }
}
