package com.infotech.lang.application.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotech.lang.application.ai.AiGateway;
import com.infotech.lang.application.ai.AiRequest;
import com.infotech.lang.application.ai.AiResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Asks the Meta model for a short, friendly written summary of a finished assessment. The model is given only the numbers that Java
 * computed (never the student's words) and must not invent anything; it cannot change a score or the level result.
 */
@Service
public class ReportSummaryWriter {

    public static final int PROMPT_VERSION = 1;
    static final int MAX_CHARS = 1200;

    static final String SYSTEM = "You write short, kind, honest feedback for a language learner after an assessment, and answer only "
            + "with the requested JSON. Use only the facts given. Never invent scores, words or advice about things not in the facts.";

    private final AiGateway gateway;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportSummaryWriter(AiGateway gateway, @Value("${meta.text.model:muse-spark-1.3}") String model) {
        this.gateway = gateway;
        this.model = model;
    }

    public String model() {
        return model;
    }

    /** The summary, or empty when the model could not be reached or its answer was unusable. Never throws. */
    public Optional<String> write(ReportSnapshot facts) {
        AiResult result;
        try {
            result = gateway.completeJson(new AiRequest("REPORT_SUMMARY", SYSTEM, task(facts), schema(), 40));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        if (!result.ok()) {
            return Optional.empty();
        }
        return parse(result.content());
    }

    Optional<String> parse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String text = root.path("summary").asText("").trim();
            if (text.isEmpty() || text.length() > MAX_CHARS) {
                return Optional.empty();
            }
            return Optional.of(text);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static String task(ReportSnapshot r) {
        String language = "hindi".equalsIgnoreCase(r.language()) ? "Hindi" : "Tamil";
        StringBuilder sb = new StringBuilder();
        sb.append("A learner has just finished a Level ").append(r.level()).append(" ").append(language)
                .append(" placement assessment. These are the facts (computed by the app; do not change them):\n");
        for (ReportSnapshot.Block b : r.blocks()) {
            sb.append("- ").append(b.title()).append(": ").append(b.correct()).append(" of ").append(b.asked()).append(" correct, ")
                    .append(Boolean.TRUE.equals(b.passed()) ? "passed" : "not passed").append('\n');
        }
        sb.append("- Overall: ").append(r.totalCorrect()).append(" of ").append(r.totalAsked()).append(" correct (")
                .append(Math.round(r.percent())).append("%). Pass mark: 75% overall.\n");
        sb.append("- Level result: ").append(r.fullAssessment()
                ? (Boolean.TRUE.equals(r.levelPassed()) ? "passed" : "not passed yet") : "partial assessment, no level result").append('\n');
        long spoken = r.questions().stream().filter(q -> q.notes().stream().anyMatch(n -> n.startsWith("Spoken"))).count();
        long spelling = r.questions().stream().filter(q -> q.notes().stream().anyMatch(n -> n.contains("spelling slips"))).count();
        sb.append("- Answers given by voice: ").append(spoken).append("; answers with spelling slips: ").append(spelling).append('\n');
        sb.append("\nWrite 3 short paragraphs in plain English (about 120 words in total): (1) what went well, naming the strongest part; ")
                .append("(2) what to practise, naming the weakest part and, if there were spelling slips, that spelling needs practice even when ")
                .append("the meaning was right; (3) one encouraging next step. Speak to the learner as \"you\". Do not list every number again. ")
                .append("Do not mention individual answers. Do not promise a level you were not told about.");
        return sb.toString();
    }

    static Map<String, Object> schema() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("type", "string");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("summary", summary);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("summary"));
        schema.put("additionalProperties", false);
        return schema;
    }
}
