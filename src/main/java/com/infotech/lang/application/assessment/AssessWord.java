package com.infotech.lang.application.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only view of a row of the existing {@code asset} table (a word with its optional picture and audio). */
@Entity
@Table(name = "asset")
public class AssessWord {

    @Id
    private Long id;

    @Column(name = "level")
    private Integer level;

    @Column(name = "question_text")
    private String questionText;

    @Column(name = "answer_text")
    private String answerText;

    @Column(name = "answer_options")
    private String answerOptions;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "curriculum_id")
    private Long curriculumId;

    @Column(name = "asset_status")
    private String assetStatus;

    @Column(name = "asset_type")
    private String assetType;

    public Long getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getAnswerText() {
        return answerText;
    }

    public String getAnswerOptions() {
        return answerOptions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }
}
