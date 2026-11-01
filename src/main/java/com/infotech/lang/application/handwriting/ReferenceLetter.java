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

    @Column(name = "category")
    private String category;

    @Column(name = "sub_category")
    private String subCategory;

    @Column(name = "display_order")
    private int displayOrder;

    public String getLetter() {
        return letter;
    }

    public String getCategory() {
        return category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public String getReferenceBlobUrl() {
        return referenceBlobUrl;
    }
}
