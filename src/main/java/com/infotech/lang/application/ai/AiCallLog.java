package com.infotech.lang.application.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** One row per model call: what, which model, how it went, how long. Never holds a prompt, an answer or a key. */
@Entity
@Table(name = "assess_ai_call_log")
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purpose", nullable = false, length = 40)
    private String purpose;

    @Column(name = "model", nullable = false, length = 60)
    private String model;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "tokens_in")
    private Integer tokensIn;

    @Column(name = "tokens_out")
    private Integer tokensOut;

    @Column(name = "error_summary", length = 300)
    private String errorSummary;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected AiCallLog() {
        // for JPA
    }

    public AiCallLog(String purpose, String model, String status, Integer httpStatus, Integer latencyMs,
                     Integer tokensIn, Integer tokensOut, String errorSummary) {
        this.purpose = purpose;
        this.model = model;
        this.status = status;
        this.httpStatus = httpStatus;
        this.latencyMs = latencyMs;
        this.tokensIn = tokensIn;
        this.tokensOut = tokensOut;
        this.errorSummary = errorSummary == null ? null
                : errorSummary.substring(0, Math.min(errorSummary.length(), 300));
    }

    public Long getId() {
        return id;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getStatus() {
        return status;
    }
}
