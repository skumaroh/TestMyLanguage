package com.infotech.lang.application.feedback;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Takes feedback from signed-in users (one every {@value #MIN_SECONDS_BETWEEN} seconds at most, text capped) and lets admins read it
 * and set the triage flags. Text is stored as typed and always shown as plain text.
 */
@Service
public class FeedbackService {

    public static final int MAX_CHARS = 2000;
    public static final int MIN_SECONDS_BETWEEN = 60;
    static final int MAX_REF_CHARS = 300;

    /** The outcome of a send; {@code message} is a short line for the user. */
    public record Result(boolean ok, String message) {
    }

    private final FeedbackRepository repository;

    public FeedbackService(FeedbackRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Result submit(UUID userId, String sourcePage, int stars, String experience, String suggestions) {
        if (userId == null) {
            return new Result(false, "Please sign in again.");
        }
        if (stars < 1 || stars > 5) {
            return new Result(false, "Please choose 1 to 5 stars.");
        }
        String exp = clean(experience);
        String sug = clean(suggestions);
        if (repository.countByUserIdAndCreatedAtAfter(userId, OffsetDateTime.now().minus(Duration.ofSeconds(MIN_SECONDS_BETWEEN))) > 0) {
            return new Result(false, "You just sent feedback. Please wait a minute before sending more.");
        }
        repository.save(new Feedback(userId, clean(sourcePage, 40), stars, exp, sug));
        return new Result(true, "Thank you! Your feedback was saved.");
    }

    @Transactional(readOnly = true)
    public List<Feedback> latest() {
        return repository.findTop500ByOrderByCreatedAtDesc();
    }

    @Transactional
    public boolean saveTriage(Long id, boolean defect, boolean ticket, String ticketRef, boolean addressed, UUID by) {
        return repository.findById(id).map(f -> {
            f.triage(defect, ticket, clean(ticketRef, MAX_REF_CHARS), addressed, by);
            return true;
        }).orElse(false);
    }

    static String clean(String s) {
        return clean(s, MAX_CHARS);
    }

    static String clean(String s, int max) {
        String t = s == null ? "" : s.strip();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }
}
