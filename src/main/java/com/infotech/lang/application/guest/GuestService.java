package com.infotech.lang.application.guest;

import com.infotech.lang.application.data.entity.Status;
import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.entity.UserRole;
import com.infotech.lang.application.data.repository.StatusRepository;
import com.infotech.lang.application.data.repository.UserRepository;
import com.infotech.lang.application.data.service.UserSessionLogService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Guest mode: makes a temporary guest account, records anonymous statistics (no user id, no address, nothing typed) and runs the
 * database clean-up. Everything that touches the new tables is wrapped so that a missing migration can never break sign-in.
 */
@Service
public class GuestService {

    private static final Logger logger = LoggerFactory.getLogger(GuestService.class);

    public static final String STARTED = "STARTED";
    public static final String MODULE_DONE = "MODULE_DONE";
    public static final String FINISHED = "FINISHED";
    public static final String SIGNIN_GOOGLE = "SIGNIN_GOOGLE";
    public static final String SIGNIN_REGISTER = "SIGNIN_REGISTER";

    private final boolean enabled;
    private final UserRepository users;
    private final StatusRepository statuses;
    private final UserSessionLogService sessions;
    private final JdbcTemplate jdbc;

    public GuestService(@Value("${guest.enabled:false}") boolean enabled, UserRepository users, StatusRepository statuses,
                        UserSessionLogService sessions, JdbcTemplate jdbc) {
        this.enabled = enabled;
        this.users = users;
        this.statuses = statuses;
        this.sessions = sessions;
        this.jdbc = jdbc;
    }

    public boolean enabled() {
        return enabled;
    }

    /** Creates the guest account (no password, no personal data) and its session record, which the clean-up relies on. */
    public User createGuest(String language, String sessionId) {
        if (!enabled) {
            throw new IllegalStateException("Guest mode is switched off");
        }
        User guest = newGuestUser();
        Optional<Status> status = statuses.findAll().stream().findFirst();
        status.ifPresent(guest::setStatus);
        User saved = users.save(guest);
        sessions.recordLogin(saved, sessionId);
        event(language, STARTED, null);
        return saved;
    }

    /** The guest account as stored: a random address, no password, no personal data. The user table requires a first and a last name. */
    static User newGuestUser() {
        User guest = new User();
        String email = GuestAccount.newEmail();
        guest.setEmail(email);
        guest.setUserName(email);
        guest.setFirstName("Guest");
        guest.setLastName("User");
        guest.setDisplayName("Guest");
        guest.setAuthProvider(GuestAccount.PROVIDER);
        guest.setEmailVerified(true);
        guest.setPassword(null);
        guest.setRole(UserRole.STUDENT);
        guest.setEnabled(true);
        guest.setTeacher(null);
        return guest;
    }

    /** One anonymous statistics row. Never throws. */
    public void event(String language, String event, String detail) {
        try {
            jdbc.update("insert into test_lang_guest_event (language, event, detail) values (?, ?, ?)", language == null ? "" : language, event, detail);
        } catch (RuntimeException e) {
            logger.warn("Could not record the guest event {} ({})", event, e.getClass().getSimpleName());
        }
    }

    /** Deletes the data of guests older than {@code hours} hours (the database function only ever touches guest accounts). Never throws. */
    public int purge(int hours) {
        try {
            Integer removed = jdbc.queryForObject("select purge_guest_data(make_interval(hours => ?))", Integer.class, hours);
            return removed == null ? 0 : removed;
        } catch (RuntimeException e) {
            logger.warn("Could not delete old guest data ({})", e.getClass().getSimpleName());
            return -1;
        }
    }

    public record DayRow(String day, String language, long started, long finished, long moduleDone, long google, long register) {
    }

    /** Guest statistics per day and language, newest first. Empty when the table is not there. */
    public List<DayRow> statsByDay(int days) {
        try {
            return jdbc.query("select to_char(created_at at time zone 'UTC', 'YYYY-MM-DD') as day, language, "
                            + "count(*) filter (where event = 'STARTED'), count(*) filter (where event = 'FINISHED'), "
                            + "count(*) filter (where event = 'MODULE_DONE'), count(*) filter (where event = 'SIGNIN_GOOGLE'), "
                            + "count(*) filter (where event = 'SIGNIN_REGISTER') "
                            + "from test_lang_guest_event where created_at > now() - make_interval(days => ?) "
                            + "group by 1, 2 order by 1 desc, 2",
                    (rs, i) -> new DayRow(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6),
                            rs.getLong(7)), days);
        } catch (RuntimeException e) {
            logger.warn("Could not read the guest statistics ({})", e.getClass().getSimpleName());
            return List.of();
        }
    }

    /** How many guests did each module, over the last {@code days} days. */
    public Map<String, Long> modulesDone(int days) {
        try {
            Map<String, Long> out = new java.util.LinkedHashMap<>();
            jdbc.query("select coalesce(detail, '?'), count(*) from test_lang_guest_event where event = 'MODULE_DONE' "
                    + "and created_at > now() - make_interval(days => ?) group by 1 order by 1",
                    rs -> {
                        out.put(rs.getString(1), rs.getLong(2));
                    }, days);
            return out;
        } catch (RuntimeException e) {
            return Map.of();
        }
    }
}
