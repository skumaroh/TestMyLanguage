package com.infotech.lang.application.proctor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Generates missing candidate texts in the background, never while a student waits. Runs once after start-up and
 * whenever a page finds a slot with nothing approved and nothing pending. A failed slot is not retried for ten minutes.
 */
@Component
public class ProctorWarmUp {

    private static final Logger logger = LoggerFactory.getLogger(ProctorWarmUp.class);
    private static final Duration COOL_DOWN = Duration.ofMinutes(10);

    private final ProctorTextService service;
    private final boolean enabled;
    private final int level;
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> failedAt = new ConcurrentHashMap<>();

    public ProctorWarmUp(ProctorTextService service,
                         @Value("${proctor.warmup.enabled:true}") boolean enabled,
                         @Value("${proctor.level:1}") int level) {
        this.service = service;
        this.enabled = enabled;
        this.level = level;
    }

    @Async("proctorExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!enabled) {
            return;
        }
        try {
            for (ProctorSlot slot : ProctorSlot.allForLevel(level, service.proctorLanguage())) {
                generateIfNeeded(slot);
            }
        } catch (RuntimeException e) {
            // A missing table (migration not run yet) or an unreachable database must never stop the app starting.
            logger.warn("Proctor warm-up stopped ({})", e.getClass().getSimpleName());
        }
    }

    @Async("proctorExecutor")
    public void requestGenerationIfNeeded(ProctorSlot slot) {
        if (!enabled) {
            return;
        }
        try {
            generateIfNeeded(slot);
        } catch (RuntimeException e) {
            logger.warn("Proctor generation stopped ({})", e.getClass().getSimpleName());
        }
    }

    private void generateIfNeeded(ProctorSlot slot) {
        String key = slot.key();
        Instant failed = failedAt.get(key);
        if (failed != null && Duration.between(failed, Instant.now()).compareTo(COOL_DOWN) < 0) {
            return;
        }
        if (!service.needsCandidate(slot) || !inFlight.add(key)) {
            return;
        }
        try {
            ProctorTextService.GenerationOutcome outcome = service.generateCandidate(slot, false);
            if (outcome == ProctorTextService.GenerationOutcome.FAILED
                    || outcome == ProctorTextService.GenerationOutcome.REJECTED) {
                failedAt.put(key, Instant.now());
            } else {
                failedAt.remove(key);
            }
        } finally {
            inFlight.remove(key);
        }
    }
}
