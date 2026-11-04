package com.infotech.lang.application.certificate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads and writes the "name on certificate" of a user. It uses plain SQL (not the user entity) so that a database without the V090
 * columns only affects the certificate pages, never sign-in. The name can be set once; only {@link #clear} (an admin action) removes it.
 */
@Service
public class CertificateNameService {

    private final JdbcTemplate jdbc;

    public CertificateNameService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> get(UUID userId) {
        List<String> found = jdbc.query("select certificate_name from \"user\" where id = ?", (rs, i) -> rs.getString(1), userId);
        return found.stream().filter(n -> n != null && !n.isBlank()).findFirst();
    }

    /** Sets the name only while none is set; false when one already is (or the user does not exist). */
    public boolean setOnce(UUID userId, String name) {
        return jdbc.update("update \"user\" set certificate_name = ?, certificate_name_set_at = now() "
                + "where id = ? and certificate_name is null", name, userId) == 1;
    }

    /** Admin only: removes the name so the learner can enter it again. False when there was none. */
    public boolean clear(UUID userId) {
        return jdbc.update("update \"user\" set certificate_name = null, certificate_name_set_at = null "
                + "where id = ? and certificate_name is not null", userId) == 1;
    }
}
