package com.infotech.lang.application.assessment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessAnswerRepository extends JpaRepository<AssessAnswer, Long> {

    /** The items (for example {@code asset:12}) already asked in any block of an attempt. */
    @Query("select a.itemRef from AssessAnswer a where a.blockResultId in "
            + "(select b.id from AssessBlockResult b where b.attemptId = :attemptId)")
    List<String> findItemRefsByAttempt(@Param("attemptId") Long attemptId);
}
