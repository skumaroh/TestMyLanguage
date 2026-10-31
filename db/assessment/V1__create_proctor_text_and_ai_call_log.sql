-- V1: Meta-written proctor texts (greeting and activity introductions) and the Meta call log.
-- Run BY HAND as flyway_user, per language database (tamil, hindi), with history table flyway_assess_history.
-- NOT run through LanguageToolDBScripts. Additive only; rollback: DROP TABLE assess_proctor_text, assess_ai_call_log.
-- Design: LanguageTool repo, docs/hackathon/feature-1-meta-proctor-design.md

CREATE TABLE IF NOT EXISTS assess_proctor_text (
    id               BIGSERIAL PRIMARY KEY,
    purpose          VARCHAR(30)  NOT NULL,                       -- GREETING, ACTIVITY_INTRO
    activity_code    VARCHAR(30)  NOT NULL DEFAULT 'NONE',        -- NONE (greeting), RECOGNITION, HANDWRITING, TRANSLATION, PERSONA, VIDEO
    level            INTEGER      NOT NULL DEFAULT 1,
    proctor_language VARCHAR(10)  NOT NULL,                       -- en, ta, hi
    body             TEXT         NOT NULL,                       -- English part, newline, short phrase in the tested language; may contain {name}
    model            VARCHAR(60)  NOT NULL,
    prompt_version   INTEGER      NOT NULL DEFAULT 1,
    source           VARCHAR(20)  NOT NULL DEFAULT 'META',        -- META (as generated) or EDITED (changed by a person)
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',     -- PENDING (candidate), APPROVED (served to students), DISCARDED
    reviewed_by      UUID REFERENCES "user"(id),
    reviewed_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- at most one APPROVED and at most one PENDING text per slot
CREATE UNIQUE INDEX IF NOT EXISTS uq_assess_proctor_text_approved
    ON assess_proctor_text (purpose, activity_code, level, proctor_language) WHERE status = 'APPROVED';
CREATE UNIQUE INDEX IF NOT EXISTS uq_assess_proctor_text_pending
    ON assess_proctor_text (purpose, activity_code, level, proctor_language) WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS assess_ai_call_log (
    id            BIGSERIAL PRIMARY KEY,
    purpose       VARCHAR(40) NOT NULL,                           -- for example PROCTOR_TEXT
    model         VARCHAR(60) NOT NULL,
    status        VARCHAR(20) NOT NULL,                           -- OK, ERROR, NOT_CONFIGURED
    http_status   INTEGER,
    latency_ms    INTEGER,
    tokens_in     INTEGER,
    tokens_out    INTEGER,
    error_summary VARCHAR(300),                                   -- never the prompt, the answer or a key
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Explicit rights (no default privileges). No DELETE: discarded texts are kept.
GRANT SELECT, INSERT, UPDATE ON assess_proctor_text TO assessment_app_user;
GRANT SELECT, INSERT         ON assess_ai_call_log  TO assessment_app_user;
GRANT USAGE ON SEQUENCE assess_proctor_text_id_seq, assess_ai_call_log_id_seq TO assessment_app_user;
