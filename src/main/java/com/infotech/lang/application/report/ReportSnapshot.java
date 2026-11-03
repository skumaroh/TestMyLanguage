package com.infotech.lang.application.report;

import java.util.List;

/**
 * The final report of one completed attempt, exactly as shown to the student. It is stored as JSON when the attempt is completed
 * so that an old report never changes if words, sentences or conversations are edited later.
 */
public record ReportSnapshot(long attemptId, String language, int level, String startedAt, String completedAt, int totalAsked,
                             int totalCorrect, double percent, Boolean levelPassed, boolean fullAssessment, String levelMessage,
                             List<Block> blocks, List<Question> questions, String summary, String summaryModel) {

    /** One block's result. */
    public record Block(String code, String title, int order, int asked, int correct, Boolean passed) {
    }

    /** One question: what was asked, what the student answered, whether it counted, what was expected, and notes. */
    public record Question(String blockCode, int position, String asked, String yourAnswer, boolean correct, String expected,
                           List<String> notes) {
    }

    public ReportSnapshot withSummary(String newSummary, String model) {
        return new ReportSnapshot(attemptId, language, level, startedAt, completedAt, totalAsked, totalCorrect, percent, levelPassed,
                fullAssessment, levelMessage, blocks, questions, newSummary, model);
    }
}
