package com.infotech.lang.application.certificate;

import com.infotech.lang.application.report.Certificate;
import com.infotech.lang.application.report.ReportSnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * The append-only certificate log ({@code test_lang_certificate}, V090): the app can only add rows and read them. A certificate gets one
 * ISSUED row the first time it is shown; that row carries a random verification code which later rows of the same attempt reuse.
 * DOWNLOAD and PRINT mean the button was clicked (a browser cannot prove paper was printed). NAME_RESET is an admin clearing a name.
 */
@Service
public class CertificateAudit {

    public static final String ISSUED = "ISSUED";
    public static final String DOWNLOAD = "DOWNLOAD";
    public static final String PRINT = "PRINT";
    public static final String NAME_RESET = "NAME_RESET";

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";   // no 0, 1, I, L, O
    private static final SecureRandom RANDOM = new SecureRandom();

    /** One row of the log, as shown to admins. */
    public record Row(OffsetDateTime when, String action, String email, String name, String number, String code, String language,
                      Integer level, BigDecimal percent) {
    }

    private final JdbcTemplate jdbc;

    public CertificateAudit(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Records the first showing of this certificate (once per attempt) and returns its verification code. */
    public String issue(ReportSnapshot r, UUID userId, String email, String name) {
        String existing = codeOf(r.attemptId());
        if (existing != null) {
            return existing;
        }
        jdbc.update("insert into test_lang_certificate (action, user_id, actor_id, email, certificate_name, attempt_id, certificate_number, "
                        + "verification_code, language, level, percent) values (?,?,?,?,?,?,?,?,?,?,?) on conflict do nothing",
                ISSUED, userId, userId, email, name, r.attemptId(), Certificate.number(r), newCode(), r.language(), r.level(), percent(r));
        return codeOf(r.attemptId());
    }

    /** A DOWNLOAD or PRINT click on this learner's certificate. */
    public void click(String action, ReportSnapshot r, UUID userId, UUID actorId, String email, String name, String code) {
        jdbc.update("insert into test_lang_certificate (action, user_id, actor_id, email, certificate_name, attempt_id, certificate_number, "
                        + "verification_code, language, level, percent) values (?,?,?,?,?,?,?,?,?,?,?)",
                action, userId, actorId, email, name, r.attemptId(), Certificate.number(r), code, r.language(), r.level(), percent(r));
    }

    /** An admin cleared this learner's certificate name. */
    public void nameReset(UUID userId, UUID adminId, String email, String clearedName) {
        jdbc.update("insert into test_lang_certificate (action, user_id, actor_id, email, certificate_name) values (?,?,?,?,?)",
                NAME_RESET, userId, adminId, email, clearedName);
    }

    /** The newest rows, optionally only those whose email contains {@code emailPart} and/or with the given action. */
    public List<Row> latest(int limit, String emailPart, String action) {
        StringBuilder sql = new StringBuilder("select created_at, action, email, certificate_name, certificate_number, verification_code, "
                + "language, level, percent from test_lang_certificate where 1=1");
        List<Object> args = new ArrayList<>();
        if (emailPart != null && !emailPart.isBlank()) {
            sql.append(" and lower(email) like ?");
            args.add("%" + emailPart.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%");
        }
        if (action != null && !action.isBlank()) {
            sql.append(" and action = ?");
            args.add(action);
        }
        sql.append(" order by id desc limit ").append(Math.max(1, Math.min(limit, 1000)));
        return jdbc.query(sql.toString(), (rs, i) -> new Row(rs.getObject(1, OffsetDateTime.class), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), (Integer) rs.getObject(8), rs.getBigDecimal(9)),
                args.toArray());
    }

    /** What the public verification page may show about a certificate: nothing about the email or the exact score. */
    public record Verified(String name, String language, int level, OffsetDateTime issued, String number) {
    }

    /** The certificate with this verification code, if one was issued. The code is matched exactly (after {@link #normalizeCode}). */
    public java.util.Optional<Verified> lookup(String code) {
        String normalized = normalizeCode(code);
        if (normalized == null) {
            return java.util.Optional.empty();
        }
        return jdbc.query("select certificate_name, language, level, created_at, certificate_number from test_lang_certificate "
                        + "where verification_code = ? and action = 'ISSUED'",
                (rs, i) -> new Verified(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getObject(4, OffsetDateTime.class),
                        rs.getString(5)), normalized).stream().findFirst();
    }

    /** Upper case, spaces and dashes ignored, regrouped as XXXX-XXXX-XXXX; null when it cannot be a code. */
    public static String normalizeCode(String text) {
        if (text == null) {
            return null;
        }
        String t = text.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        if (!t.matches("[A-HJKMNP-Z2-9]{12}")) {
            return null;
        }
        return t.substring(0, 4) + "-" + t.substring(4, 8) + "-" + t.substring(8, 12);
    }

    private String codeOf(long attemptId) {
        List<String> found = jdbc.query("select verification_code from test_lang_certificate where attempt_id = ? and action = 'ISSUED'",
                (rs, i) -> rs.getString(1), attemptId);
        return found.isEmpty() ? null : found.get(0);
    }

    private static BigDecimal percent(ReportSnapshot r) {
        return BigDecimal.valueOf(r.percent()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Twelve random characters in groups of four, for example {@code K7QM-2XH9-WD4P}. */
    static String newCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
