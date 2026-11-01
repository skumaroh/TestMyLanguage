package com.infotech.lang.application.assessment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessAttemptRepository extends JpaRepository<AssessAttempt, Long> {

    List<AssessAttempt> findByUserIdAndStatus(UUID userId, String status);

    java.util.Optional<AssessAttempt> findFirstByUserIdAndStatusOrderByIdDesc(UUID userId, String status);
}
