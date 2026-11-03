package com.infotech.lang.application.feedback;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findTop500ByOrderByCreatedAtDesc();

    long countByUserIdAndCreatedAtAfter(UUID userId, OffsetDateTime since);
}
