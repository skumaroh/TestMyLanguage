package com.infotech.lang.application.proctor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotech.lang.application.ai.AiGateway;
import com.infotech.lang.application.ai.AiResult;
import com.infotech.lang.application.config.LanguageContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The "librarian" for the proctor's words. Other parts of the app ask it for a text; it returns the approved one,
 * or a built-in fallback. When a slot has no candidate it can ask {@link AiGateway} to have the model write one,
 * validates it, and stores it as a PENDING candidate for an admin to approve or discard.
 */
@Service
public class ProctorTextService {

    public enum GenerationOutcome { CREATED, SKIPPED, REJECTED, FAILED }

    /** A text ready to show. {@code fromMeta} is false when it is the built-in fallback. */
    public record ResolvedText(String text, boolean fromMeta) {
    }

    private static final Logger logger = LoggerFactory.getLogger(ProctorTextService.class);

    private final ProctorTextRepository repository;
    private final AiGateway gateway;
    private final ProctorTextValidator validator;
    private final LanguageContext languageContext;
    private final String model;
    private final String proctorLanguage;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProctorTextService(ProctorTextRepository repository,
                              AiGateway gateway,
                              ProctorTextValidator validator,
                              LanguageContext languageContext,
                              @Value("${meta.text.model:muse-spark-1.3}") String model,
                              @Value("${proctor.language:en}") String proctorLanguage) {
        this.repository = repository;
        this.gateway = gateway;
        this.validator = validator;
        this.languageContext = languageContext;
        this.model = model;
        this.proctorLanguage = proctorLanguage;
    }

    public String proctorLanguage() {
        return proctorLanguage;
    }

    public String testedLanguage() {
        return languageContext.getLanguage();
    }

    /** The approved text for the slot with the student's name filled in, or the fallback. Never throws. */
    public ResolvedText resolve(ProctorSlot slot, String studentName) {
        try {
            List<ProctorText> approved = repository.findBySlot(slot.purpose(), slot.activity(), slot.level(),
                    slot.proctorLanguage(), ProctorStatus.APPROVED);
            if (!approved.isEmpty()) {
                return new ResolvedText(fill(approved.get(0).getBody(), studentName), true);
            }
        } catch (RuntimeException e) {
            logger.warn("Could not read proctor text for {} ({})", slot.key(), e.getClass().getSimpleName());
        }
        return new ResolvedText(fill(ProctorFallbacks.forSlot(slot), studentName), false);
    }

    /** True when the slot has neither an approved text nor a pending candidate. Never throws. */
    public boolean needsCandidate(ProctorSlot slot) {
        try {
            return repository.findBySlot(slot.purpose(), slot.activity(), slot.level(), slot.proctorLanguage(),
                    ProctorStatus.APPROVED).isEmpty()
                    && repository.findBySlot(slot.purpose(), slot.activity(), slot.level(), slot.proctorLanguage(),
                    ProctorStatus.PENDING).isEmpty();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Asks the model for a new candidate. Skips when a candidate is already waiting for review, or (unless
     * {@code replaceApproved}) when an approved text exists. Calls the model outside any database transaction.
     */
    public GenerationOutcome generateCandidate(ProctorSlot slot, boolean replaceApproved) {
        try {
            if (!repository.findBySlot(slot.purpose(), slot.activity(), slot.level(), slot.proctorLanguage(),
                    ProctorStatus.PENDING).isEmpty()) {
                return GenerationOutcome.SKIPPED;
            }
            if (!replaceApproved && !repository.findBySlot(slot.purpose(), slot.activity(), slot.level(),
                    slot.proctorLanguage(), ProctorStatus.APPROVED).isEmpty()) {
                return GenerationOutcome.SKIPPED;
            }
        } catch (RuntimeException e) {
            logger.warn("Could not check proctor text for {} ({})", slot.key(), e.getClass().getSimpleName());
            return GenerationOutcome.FAILED;
        }

        AiResult result = gateway.completeJson(ProctorPromptBuilder.build(slot, testedLanguage()));
        if (!result.ok()) {
            logger.warn("Meta call failed for {} ({})", slot.key(), result.error());
            return GenerationOutcome.FAILED;
        }
        String body = toBody(result.content());
        if (body == null) {
            logger.warn("Meta answer for {} was not the expected JSON", slot.key());
            return GenerationOutcome.REJECTED;
        }
        List<String> problems = validator.validate(body, testedLanguage());
        if (!problems.isEmpty()) {
            logger.warn("Meta text for {} rejected: {}", slot.key(), String.join("; ", problems));
            return GenerationOutcome.REJECTED;
        }
        try {
            repository.save(new ProctorText(slot, body, model, ProctorPromptBuilder.PROMPT_VERSION));
            return GenerationOutcome.CREATED;
        } catch (DataIntegrityViolationException e) {
            return GenerationOutcome.SKIPPED; // another pod created the candidate first
        } catch (RuntimeException e) {
            logger.warn("Could not store proctor text for {} ({})", slot.key(), e.getClass().getSimpleName());
            return GenerationOutcome.FAILED;
        }
    }

    @Transactional
    public void approve(long id, UUID reviewer) {
        ProctorText text = requirePending(id);
        for (ProctorText existing : repository.findBySlot(text.getPurpose(), text.getActivity(), text.getLevel(),
                text.getProctorLanguage(), ProctorStatus.APPROVED)) {
            existing.setStatus(ProctorStatus.DISCARDED);
            existing.setReviewedBy(reviewer);
            existing.setReviewedAt(OffsetDateTime.now());
            repository.saveAndFlush(existing); // free the one-approved-per-slot index before approving this one
        }
        text.setStatus(ProctorStatus.APPROVED);
        text.setReviewedBy(reviewer);
        text.setReviewedAt(OffsetDateTime.now());
        repository.save(text);
    }

    @Transactional
    public void discard(long id, UUID reviewer) {
        ProctorText text = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("no such text"));
        if (text.getStatus() == ProctorStatus.DISCARDED) {
            return;
        }
        text.setStatus(ProctorStatus.DISCARDED);
        text.setReviewedBy(reviewer);
        text.setReviewedAt(OffsetDateTime.now());
        repository.save(text);
    }

    /** Fixes the wording of a candidate and approves it. The edited text must pass the same checks. */
    @Transactional
    public void editAndApprove(long id, String newBody, UUID reviewer) {
        List<String> problems = validator.validate(newBody, testedLanguage());
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", problems));
        }
        ProctorText text = requirePending(id);
        text.setBody(newBody.trim());
        text.setSource("EDITED");
        repository.save(text);
        approve(id, reviewer);
    }

    public List<ProctorText> listAll() {
        return repository.findAllForReview();
    }

    private ProctorText requirePending(long id) {
        ProctorText text = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("no such text"));
        if (text.getStatus() != ProctorStatus.PENDING) {
            throw new IllegalStateException("only a pending candidate can be approved");
        }
        return text;
    }

    /** Joins the two parts of the model's answer into the stored body, or returns null if it is not usable. */
    String toBody(String content) {
        try {
            String json = content.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "");
            }
            JsonNode node = mapper.readTree(json);
            String english = node.path("english").asText("").trim();
            String phrase = node.path("phrase").asText("").trim();
            if (english.isEmpty() || phrase.isEmpty()) {
                return null;
            }
            return english + "\n" + phrase;
        } catch (Exception e) {
            return null;
        }
    }

    static String fill(String body, String studentName) {
        String name = studentName == null ? "" : studentName.replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            name = "there";
        }
        if (name.length() > 40) {
            name = name.substring(0, 40);
        }
        return body.replace("{name}", name);
    }

    /** For tests and the review page. */
    Optional<ProctorText> find(long id) {
        return repository.findById(id);
    }
}
