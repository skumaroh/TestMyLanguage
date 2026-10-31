package com.infotech.lang.application.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only view of the existing {@code letters} table: one row holding the language's letters, comma separated. */
@Entity
@Table(name = "letters")
public class AssessLetters {

    @Id
    private Long id;

    @Column(name = "letters", columnDefinition = "text")
    private String letters;

    public String getLetters() {
        return letters;
    }
}
