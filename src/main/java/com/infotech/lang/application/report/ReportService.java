package com.infotech.lang.application.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotech.lang.application.assessment.AssessAnswer;
import com.infotech.lang.application.assessment.AssessAnswerRepository;
import com.infotech.lang.application.assessment.AssessAttempt;
import com.infotech.lang.application.assessment.AssessAttemptRepository;
import com.infotech.lang.application.assessment.AssessBlockResult;
import com.infotech.lang.application.assessment.AssessBlockResultRepository;
import com.infotech.lang.application.assessment.AssessWordRepository;
import com.infotech.lang.application.conversation.ConversationObjective;
import com.infotech.lang.application.conversation.ConversationObjectiveRepository;
import com.infotech.lang.application.translation.SentencePicker;
import com.infotech.lang.application.translation.TranslationItem;
import com.infotech.lang.application.video.VideoClipRepository;
import com.infotech.lang.application.video.VideoPicker;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Completes an attempt and builds, stores and loads its final report. Completing fixes the totals, the percent and the level result on
 * the attempt (75% overall; a partial attempt has no level result), then stores a snapshot of the report with the Meta-written summary
 * in {@code assess_report}. A stored report never changes. If that table is not there yet the report is built live without a stored
 * summary, so nothing breaks while the migration is pending.
 */
@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final AssessAttemptRepository attempts;
    private final AssessBlockResultRepository blocks;
    private final AssessAnswerRepository answers;
    private final AssessWordRepository words;
    private final SentencePicker sentences;
    private final ConversationObjectiveRepository objectives;
    private final VideoClipRepository videoClips;
    private final ReportSummaryWriter writer;
    private final VideoPicker videoPicker;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportService(AssessAttemptRepository attempts, AssessBlockResultRepository blocks, AssessAnswerRepository answers,
                         AssessWordRepository words, SentencePicker sentences, ConversationObjectiveRepository objectives,
                         VideoClipRepository videoClips, ReportSummaryWriter writer, JdbcTemplate jdbc, VideoPicker videoPicker) {
        this.attempts = attempts;
        this.blocks = blocks;
        this.answers = answers;
        this.words = words;
        this.sentences = sentences;
        this.objectives = objectives;
        this.videoClips = videoClips;
        this.writer = writer;
        this.jdbc = jdbc;
        this.videoPicker = videoPicker;
    }

    /** Whether the video part can run for this attempt's language and level (false when it cannot be checked). */
    private boolean videoAvailable(AssessAttempt attempt) {
        try {
            return videoPicker.isAvailable(attempt.getLanguage(), attempt.getLevel());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Completes the attempt (once) and stores its report. Safe to call again: a completed attempt is not changed. Not wrapped in one
     * transaction on purpose, so a missing report table cannot undo the completion.
     */
    public Optional<ReportSnapshot> complete(Long attemptId) {
        AssessAttempt attempt = attempts.findById(attemptId).orElse(null);
        if (attempt == null) {
            return Optional.empty();
        }
        if (!"COMPLETED".equals(attempt.getStatus())) {
            List<AssessBlockResult> results = blocks.findByAttemptIdOrderByBlockOrder(attemptId);
            int asked = results.stream().mapToInt(AssessBlockResult::getAsked).sum();
            int correct = results.stream().mapToInt(AssessBlockResult::getCorrect).sum();
            boolean full = ReportMath.isFull(results.stream().map(AssessBlockResult::getBlockCode).toList(), videoAvailable(attempt));
            attempt.complete(asked, correct, ReportMath.percent(asked, correct), ReportMath.levelPassed(full, asked, correct));
            attempts.save(attempt);
        }
        Optional<ReportSnapshot> stored = readStored(attemptId);
        if (stored.isPresent()) {
            return stored;
        }
        ReportSnapshot built = build(attempt);
        Optional<String> summary = writer.write(built);
        ReportSnapshot withSummary = summary.map(s -> built.withSummary(s, writer.model())).orElse(built);
        store(withSummary);
        return Optional.of(withSummary);
    }

    /** The report of a completed attempt: the stored snapshot, or a live one when none is stored (no summary). */
    public Optional<ReportSnapshot> load(Long attemptId) {
        AssessAttempt attempt = attempts.findById(attemptId).orElse(null);
        if (attempt == null || !"COMPLETED".equals(attempt.getStatus())) {
            return Optional.empty();
        }
        Optional<ReportSnapshot> stored = readStored(attemptId);
        return stored.isPresent() ? stored : Optional.of(build(attempt));
    }

    public Optional<AssessAttempt> attempt(Long attemptId) {
        return attempts.findById(attemptId);
    }

    /** Admin progress report: the newest attempts of all students, finished or not. */
    public List<AssessAttempt> allAttempts() {
        return attempts.findTop500ByOrderByIdDesc();
    }

    /** A student's completed attempts, newest first. */
    public List<AssessAttempt> history(UUID userId) {
        return attempts.findByUserIdAndStatusOrderByIdDesc(userId, "COMPLETED");
    }

    ReportSnapshot build(AssessAttempt attempt) {
        List<AssessBlockResult> results = blocks.findByAttemptIdOrderByBlockOrder(attempt.getId());
        List<ReportSnapshot.Block> reportBlocks = new ArrayList<>();
        List<ReportSnapshot.Question> questions = new ArrayList<>();
        for (AssessBlockResult b : results) {
            reportBlocks.add(new ReportSnapshot.Block(b.getBlockCode(), ReportMath.title(b.getBlockCode()), b.getBlockOrder(),
                    b.getAsked(), b.getCorrect(), b.getPassed()));
            for (AssessAnswer a : answers.findByBlockResultIdOrderByPosition(b.getId())) {
                questions.add(question(attempt.getLanguage(), b.getBlockCode(), a));
            }
        }
        int asked = results.stream().mapToInt(AssessBlockResult::getAsked).sum();
        int correct = results.stream().mapToInt(AssessBlockResult::getCorrect).sum();
        boolean full = ReportMath.isFull(results.stream().map(AssessBlockResult::getBlockCode).toList(), videoAvailable(attempt));
        BigDecimal percent = attempt.getPercent() != null ? attempt.getPercent() : ReportMath.percent(asked, correct);
        Boolean levelPassed = "COMPLETED".equals(attempt.getStatus()) ? attempt.getLevelPassed() : ReportMath.levelPassed(full, asked, correct);
        return new ReportSnapshot(attempt.getId(), attempt.getLanguage(), attempt.getLevel(),
                String.valueOf(attempt.getStartedAt()), String.valueOf(attempt.getCompletedAt()), asked, correct, percent.doubleValue(),
                levelPassed, full, ReportMath.levelMessage(levelPassed, attempt.getLevel(), full), reportBlocks, questions, null, null);
    }

    private ReportSnapshot.Question question(String language, String blockCode, AssessAnswer a) {
        AnswerLog.Parsed log = AnswerLog.parse(a.getAnswerGiven());
        List<String> notes = new ArrayList<>(log.notes());
        String ref = a.getItemRef() == null ? "" : a.getItemRef();
        String[] parts = ref.split(":");
        String asked = "";
        String expected = null;
        String yours = log.answer();
        switch (blockCode) {
            case "WORDS", "AUDIO_WORDS" -> {
                asked = "WORDS".equals(blockCode) ? "Build the word" : "Listen and build the word";
                expected = parts.length > 1 ? wordAnswer(parts[1]) : null;
            }
            case "VIDEO" -> {
                asked = "What is happening in the clip?";
                var clip = parts.length > 1 ? clipOf(parts[1]) : null;
                if (clip != null) {
                    asked += " (" + clip.getEnglish() + ")";
                    expected = clip.getExpectedText();
                }
                if (!log.spellingCorrected().isBlank()) {
                    notes.add("Correct spelling: " + log.spellingCorrected());
                }
            }
            case "DRAW_LETTERS" -> {
                asked = "Draw the letter " + (parts.length > 1 ? parts[1] : "");
                expected = parts.length > 1 ? parts[1] : null;
                yours = "(your drawing)";
            }
            case "TRANSLATION" -> {
                TranslationItem item = parts.length > 1 ? sentence(language, parts[1]) : null;
                asked = item == null ? "" : item.english();
                expected = item != null && item.hasAnswer() ? item.expected() : null;
                if (!log.spellingCorrected().isBlank()) {
                    notes.add("Correct spelling: " + log.spellingCorrected());
                }
            }
            case "CONVERSATION" -> {
                asked = log.asked();
                if (parts.length > 2) {
                    String goal = objectiveGoal(parts[1], parts[2]);
                    if (goal != null) {
                        notes.add("Goal: " + goal);
                    }
                }
                if (!log.reason().isBlank()) {
                    notes.add(log.reason());
                }
                if (!log.spellingCorrected().isBlank()) {
                    notes.add("Correct spelling: " + log.spellingCorrected());
                }
            }
            default -> {
                // an unknown block: show what was stored
            }
        }
        if (log.skipped()) {
            yours = "(no answer)";
            notes.add("DONE".equals(log.skipReason()) ? "You ended the conversation" : "The conversation ended after a quiet period");
        }
        return new ReportSnapshot.Question(blockCode, a.getPosition(), asked, yours, a.isCorrect(), expected, notes);
    }

    private String wordAnswer(String id) {
        try {
            return words.findById(Long.parseLong(id)).map(w -> w.getAnswerText()).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private com.infotech.lang.application.video.VideoClip clipOf(String id) {
        try {
            return videoClips.findById(Long.parseLong(id)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private TranslationItem sentence(String language, String id) {
        try {
            long wanted = Long.parseLong(id);
            return sentences.allFor(language).stream().filter(i -> i.id() == wanted).findFirst().orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String objectiveGoal(String conversationId, String position) {
        try {
            int wanted = Integer.parseInt(position);
            return objectives.findByConversationIdOrderByPosition(Long.parseLong(conversationId)).stream()
                    .filter(o -> o.getPosition() == wanted).map(ConversationObjective::getObjectiveEn).findFirst().orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Optional<ReportSnapshot> readStored(Long attemptId) {
        try {
            List<String> rows = jdbc.queryForList("select snapshot_json from assess_report where attempt_id = ?", String.class, attemptId);
            return rows.isEmpty() ? Optional.empty() : Optional.of(mapper.readValue(rows.get(0), ReportSnapshot.class));
        } catch (DataAccessException | JsonProcessingException e) {
            return Optional.empty(); // table not there yet, or an unreadable row: build the report live instead
        }
    }

    private void store(ReportSnapshot snapshot) {
        try {
            jdbc.update("insert into assess_report (attempt_id, snapshot_json, summary_text, summary_model, summary_prompt_version) "
                            + "values (?, ?, ?, ?, ?) on conflict (attempt_id) do nothing",
                    snapshot.attemptId(), mapper.writeValueAsString(snapshot), snapshot.summary(), snapshot.summaryModel(),
                    snapshot.summary() == null ? null : ReportSummaryWriter.PROMPT_VERSION);
        } catch (DataAccessException | JsonProcessingException e) {
            logger.warn("Could not store the report ({})", e.getClass().getSimpleName());
        }
    }

    static OffsetDateTime parseTime(String text) {
        try {
            return OffsetDateTime.parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
