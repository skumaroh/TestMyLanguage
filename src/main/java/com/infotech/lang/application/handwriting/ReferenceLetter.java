package com.infotech.lang.application.handwriting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only view of a row of the existing {@code writing_letter} table: a letter and its reference image address. */
@Entity
@Table(name = "writing_letter")
public class ReferenceLetter {

    @Id
    private Long id;

    @Column(name = "letter")
    private String letter;

    @Column(name = "published")
    private boolean published;

    @Column(name = "reference_blob_url", columnDefinition = "text")
    private String referenceBlobUrl;

    @Column(name = "curriculum_id")
    private Long curriculumId;

    public String getReferenceBlobUrl() {
        return referenceBlobUrl;
    }
}
