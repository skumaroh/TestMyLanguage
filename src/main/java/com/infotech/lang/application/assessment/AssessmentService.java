package com.infotech.lang.application.assessment;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores attempts, blocks and answers. Scoring decisions are made by {@link BlockEngine}, not here. */
@Service
public class AssessmentService {

    private final AssessAttemptRepository attempts;
    private final AssessBlockResultRepository blocks;
    private final AssessAnswerRepository answers;
    private final com.infotech.lang.application.data.repository.UserRepository users;
    private final com.infotech.lang.application.guest.GuestService guests;

    public AssessmentService(AssessAttemptRepository attempts, AssessBlockResultRepository blocks,
                             AssessAnswerRepository answers,
                             com.infotech.lang.application.data.repository.UserRepository users,
                             com.infotech.lang.application.guest.GuestService guests) {
        this.attempts = attempts;
        this.blocks = blocks;
        this.answers = answers;
        this.users = users;
        this.guests = guests;
    }

    /** Starts a new attempt; an earlier unfinished attempt of the student is marked INCOMPLETE (no resume). */
    @Transactional
    public AssessAttempt startAttempt(UUID userId, String language, int level) {
        for (AssessAttempt old : attempts.findByUserIdAndStatus(userId, "IN_PROGRESS")) {
            old.setStatus("INCOMPLETE");
            old.setCompletedAt(OffsetDateTime.now());
            attempts.save(old);
        }
        AssessAttempt attempt = new AssessAttempt();
        attempt.setUserId(userId);
        attempt.setLanguage(language == null ? "" : language);
        attempt.setLevel(level);
        return attempts.save(attempt);
    }

    @Transactional
    public AssessBlockResult startBlock(Long attemptId, String blockCode, int blockOrder) {
        AssessBlockResult block = new AssessBlockResult();
        block.setAttemptId(attemptId);
        block.setBlockCode(blockCode);
        block.setBlockOrder(blockOrder);
        return blocks.save(block);
    }

    /** Stores one answer and updates the block's running counts. */
    @Transactional
    public void recordAnswer(Long blockResultId, int position, String itemRef, String given, boolean correct, int durationMs) {
        AssessAnswer answer = new AssessAnswer();
        answer.setBlockResultId(blockResultId);
        answer.setPosition(position);
        answer.setItemRef(itemRef);
        answer.setAnswerGiven(given);
        answer.setCorrect(correct);
        answer.setDurationMs(durationMs);
        answers.save(answer);
    }

    @Transactional
    public void finishBlock(Long blockResultId, int asked, int correct, boolean passed) {
        AssessBlockResult block = blocks.findById(blockResultId).orElseThrow();
        block.setAsked(asked);
        block.setCorrect(correct);
        block.setPassed(passed);
        block.setFinishedAt(OffsetDateTime.now());
        blocks.save(block);
        guestModuleDone(block);
    }

    /** For a guest only: one anonymous statistics row saying which module was finished. Never throws. */
    private void guestModuleDone(AssessBlockResult block) {
        try {
            AssessAttempt attempt = attempts.findById(block.getAttemptId()).orElse(null);
            if (attempt == null) {
                return;
            }
            users.findById(attempt.getUserId()).ifPresent(u -> {
                if (com.infotech.lang.application.guest.GuestAccount.isGuestEmail(u.getEmail())) {
                    guests.event(attempt.getLanguage(), com.infotech.lang.application.guest.GuestService.MODULE_DONE, block.getBlockCode());
                }
            });
        } catch (RuntimeException e) {
            // statistics must never break an assessment
        }
    }

    /** The student's attempt in progress (started by the first block), or a new one if there is none. */
    @Transactional
    public AssessAttempt currentOrNewAttempt(UUID userId, String language, int level) {
        return attempts.findFirstByUserIdAndStatusOrderByIdDesc(userId, "IN_PROGRESS")
                .orElseGet(() -> startAttempt(userId, language, level));
    }

    /** The items already asked in this attempt, so a later block can avoid repeating them. */
    @Transactional(readOnly = true)
    public java.util.Set<String> itemRefsInAttempt(Long attemptId) {
        return new java.util.HashSet<>(answers.findItemRefsByAttempt(attemptId));
    }
}
