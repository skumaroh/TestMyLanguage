package com.infotech.lang.application.proctor;

public enum ProctorStatus {
    /** A candidate written by the model: students never see it until an admin approves it. */
    PENDING,
    /** The text students see. At most one per slot. */
    APPROVED,
    /** Rejected by an admin; kept for the record. */
    DISCARDED
}
