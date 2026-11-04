package com.infotech.lang.application.assessment;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Which modules the student has finished in the attempt in progress, and whether they have finished an attempt before. */
@Service
public class ModuleProgressService {

    /** {@code attemptId} is null when no attempt is in progress. */
    public record Progress(Long attemptId, Set<String> finished, boolean hasCompletedBefore) {
    }

    private final AssessAttemptRepository attempts;
    private final AssessBlockResultRepository blocks;

    public ModuleProgressService(AssessAttemptRepository attempts, AssessBlockResultRepository blocks) {
        this.attempts = attempts;
        this.blocks = blocks;
    }

    @Transactional(readOnly = true)
    public Progress of(UUID userId) {
        boolean before = !attempts.findByUserIdAndStatusOrderByIdDesc(userId, "COMPLETED").isEmpty();
        return attempts.findFirstByUserIdAndStatusOrderByIdDesc(userId, "IN_PROGRESS")
                .map(a -> new Progress(a.getId(), blocks.findByAttemptIdOrderByBlockOrder(a.getId()).stream()
                        .filter(b -> b.getFinishedAt() != null).map(AssessBlockResult::getBlockCode).collect(Collectors.toSet()), before))
                .orElse(new Progress(null, Set.of(), before));
    }
}
