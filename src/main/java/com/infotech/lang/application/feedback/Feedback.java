package com.infotech.lang.application.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** One feedback a signed-in user sent, with the flags an admin sets while triaging it. Feedback is never deleted. */
@Entity
@Table(name = "test_lang_feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "source_page", length = 40)
    private String sourcePage;

    @Column(name = "stars", nullable = false)
    private int stars;

    @Column(name = "experience", columnDefinition = "text")
    private String experience;

    @Column(name = "suggestions", columnDefinition = "text")
    private String suggestions;

    @Column(name = "is_defect", nullable = false)
    private boolean defect;

    @Column(name = "is_ticket", nullable = false)
    private boolean ticket;

    @Column(name = "ticket_ref", length = 300)
    private String ticketRef;

    @Column(name = "is_addressed", nullable = false)
    private boolean addressed;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Feedback() {
    }

    public Feedback(UUID userId, String sourcePage, int stars, String experience, String suggestions) {
        this.userId = userId;
        this.sourcePage = sourcePage;
        this.stars = stars;
        this.experience = experience;
        this.suggestions = suggestions;
    }

    /** The admin's triage flags; {@code by} is the admin who saved them. */
    public void triage(boolean defect, boolean ticket, String ticketRef, boolean addressed, UUID by) {
        this.defect = defect;
        this.ticket = ticket;
        this.ticketRef = ticketRef;
        this.addressed = addressed;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = by;
    }

    public Long getId() { return id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getUserId() { return userId; }
    public String getSourcePage() { return sourcePage; }
    public int getStars() { return stars; }
    public String getExperience() { return experience; }
    public String getSuggestions() { return suggestions; }
    public boolean isDefect() { return defect; }
    public boolean isTicket() { return ticket; }
    public String getTicketRef() { return ticketRef; }
    public boolean isAddressed() { return addressed; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}
