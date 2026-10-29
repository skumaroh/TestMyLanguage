package com.infotech.lang.application.data.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.infotech.lang.application.data.entity.User;
import com.infotech.lang.application.data.entity.UserSessionLog;
import com.infotech.lang.application.data.repository.UserSessionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Tracks login/logout activity per {@code HttpSession} so the admin
 * login-activity report can show login time, logout time, and duration.
 */
@Service
public class UserSessionLogService {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionLogService.class);

    private final UserSessionLogRepository repository;
    private final Clock clock;

    @Autowired
    public UserSessionLogService(UserSessionLogRepository repository) {
        this(repository, Clock.systemUTC());
    }

    UserSessionLogService(UserSessionLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void recordLogin(User user, String sessionId) {
        UserSessionLog log = new UserSessionLog();
        log.setUser(user);
        log.setSessionId(sessionId);
        log.setLoginTime(OffsetDateTime.now(clock));
        repository.save(log);
    }

    public void recordLogout(String sessionId) {
        Optional<UserSessionLog> openSession =
                repository.findFirstBySessionIdAndLogoutTimeIsNullOrderByLoginTimeDesc(sessionId);
        if (openSession.isEmpty()) {
            logger.debug("No open session log found for session id {}; skipping logout record.", sessionId);
            return;
        }

        UserSessionLog log = openSession.get();
        log.setLogoutTime(OffsetDateTime.now(clock));
        repository.save(log);
    }

    public List<UserSessionLog> findForReport(User user, LocalDate startDate, LocalDate endDate, ZoneId zone) {
        OffsetDateTime start = startDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = endDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return repository.findForReport(user != null ? user.getId() : null, start, end);
    }

    public List<UserSessionLog> findForReportByUsers(Collection<UUID> userIds, LocalDate startDate, LocalDate endDate, ZoneId zone) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        OffsetDateTime start = startDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = endDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return repository.findForReportByUsers(userIds, start, end);
    }

    public void deleteSessionLogs(Collection<UserSessionLog> logs) {
        repository.deleteAll(logs);
    }
}
