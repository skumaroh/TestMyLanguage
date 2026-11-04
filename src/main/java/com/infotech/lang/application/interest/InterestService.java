package com.infotech.lang.application.interest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * The "next level" interest poll ({@code test_lang_interest}, V091): one row per learner, language and level, which the learner can change
 * later. Plain SQL so that a missing table only affects this form. The learner's agreement to be contacted ({@code contact_ok}) is recorded
 * with the time it was given; without it the answers are product feedback only and the email must not be used to contact anyone.
 */
@Service
public class InterestService {

    public static final List<String> INTERESTS = List.of("VERY", "MAYBE", "NOT_NOW");
    public static final int MAX_TEXT = 2000;

    /** What a learner answered. */
    public record Response(String interest, String topics, String opinion, String suggestions, boolean contactOk) {
    }

    /** One row for the admin page. */
    public record Row(OffsetDateTime updated, String name, String email, String language, String interest, String topics, String opinion,
                      String suggestions, boolean contactOk) {
    }

    private final JdbcTemplate jdbc;

    public InterestService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Response> get(UUID userId, String language, int level) {
        return jdbc.query("select interest, topics, opinion, suggestions, contact_ok from test_lang_interest "
                        + "where user_id = ? and language = ? and level_completed = ?",
                (rs, i) -> new Response(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5)),
                userId, language, level).stream().findFirst();
    }

    /** Adds the answer or replaces the learner's earlier one. Returns false (and saves nothing) when the answer is not valid. */
    public boolean save(UUID userId, String language, int level, Long attemptId, String name, String email, String interest,
                        List<String> topics, String opinion, String suggestions, boolean contactOk) {
        if (userId == null || !INTERESTS.contains(interest)) {
            return false;
        }
        jdbc.update("insert into test_lang_interest (user_id, language, level_completed, attempt_id, name, email, interest, topics, opinion, "
                        + "suggestions, contact_ok, contact_ok_at) values (?,?,?,?,?,?,?,?,?,?,?, case when ? then now() else null end) "
                        + "on conflict (user_id, language, level_completed) do update set attempt_id = excluded.attempt_id, "
                        + "name = excluded.name, email = excluded.email, interest = excluded.interest, topics = excluded.topics, "
                        + "opinion = excluded.opinion, suggestions = excluded.suggestions, updated_at = now(), "
                        + "contact_ok_at = case when excluded.contact_ok and not test_lang_interest.contact_ok then now() "
                        + "when excluded.contact_ok then test_lang_interest.contact_ok_at else null end, contact_ok = excluded.contact_ok",
                userId, language, level, attemptId, clip(name, 200), clip(email, 255), interest, clip(String.join(",", topics), 1000),
                clip(opinion, MAX_TEXT), clip(suggestions, MAX_TEXT), contactOk, contactOk);
        return true;
    }

    public List<Row> latest(int limit) {
        return jdbc.query("select updated_at, name, email, language, interest, topics, opinion, suggestions, contact_ok "
                        + "from test_lang_interest order by updated_at desc limit " + Math.max(1, Math.min(limit, 1000)),
                (rs, i) -> new Row(rs.getObject(1, OffsetDateTime.class), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getBoolean(9)));
    }

    /** Counts per interest: {very, maybe, notNow, willingToBeContacted}. */
    public int[] counts() {
        List<Row> rows = latest(1000);
        int[] c = new int[4];
        for (Row r : rows) {
            c[switch (r.interest() == null ? "" : r.interest()) {
                case "VERY" -> 0;
                case "MAYBE" -> 1;
                default -> 2;
            }]++;
            if (r.contactOk()) {
                c[3]++;
            }
        }
        return c;
    }

    static String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }

    /** The topics as a list again (they are stored separated by commas). */
    public static List<String> topicList(String stored) {
        List<String> out = new ArrayList<>();
        if (stored != null) {
            for (String t : stored.split(",")) {
                if (!t.isBlank()) {
                    out.add(t.trim());
                }
            }
        }
        return out;
    }
}
