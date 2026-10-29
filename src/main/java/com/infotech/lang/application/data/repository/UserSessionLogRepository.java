package com.infotech.lang.application.data.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.infotech.lang.application.data.entity.UserSessionLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionLogRepository extends JpaRepository<UserSessionLog, UUID> {

    Optional<UserSessionLog> findFirstBySessionIdAndLogoutTimeIsNullOrderByLoginTimeDesc(String sessionId);

    @EntityGraph(attributePaths = "user")
    @Query("select s from UserSessionLog s where (:userId is null or s.user.id = :userId) " +
            "and s.loginTime >= :start and s.loginTime < :end order by s.loginTime desc")
    List<UserSessionLog> findForReport(@Param("userId") UUID userId,
            @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @EntityGraph(attributePaths = "user")
    @Query("select s from UserSessionLog s where s.user.id in :userIds " +
            "and s.loginTime >= :start and s.loginTime < :end order by s.loginTime desc")
    List<UserSessionLog> findForReportByUsers(@Param("userIds") Collection<UUID> userIds,
            @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
