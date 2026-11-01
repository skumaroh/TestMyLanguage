package com.infotech.lang.application.translation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only view of a row of the existing {@code sentence_example} table: an English sentence and its target-language sentence. */
@Entity
@Table(name = "sentence_example")
public class TranslationSentence {

    @Id
    private Long id;

    @Column(name = "language_code")
    private String languageCode;

    @Column(name = "sentence_text", columnDefinition = "text")
    private String sentenceText;

    @Column(name = "natural_translation", columnDefinition = "text")
    private String naturalTranslation;

    @Column(name = "review_status")
    private String reviewStatus;

    public Long getId() {
        return id;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getSentenceText() {
        return sentenceText;
    }

    public String getNaturalTranslation() {
        return naturalTranslation;
    }
}
