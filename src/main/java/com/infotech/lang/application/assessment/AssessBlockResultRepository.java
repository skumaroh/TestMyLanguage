package com.infotech.lang.application.assessment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessBlockResultRepository extends JpaRepository<AssessBlockResult, Long> {

    List<AssessBlockResult> findByAttemptIdOrderByBlockOrder(Long attemptId);
}
