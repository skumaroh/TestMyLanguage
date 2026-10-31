package com.infotech.lang.application.proctor;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProctorTextRepository extends JpaRepository<ProctorText, Long> {

    @Query("select t from ProctorText t where t.purpose = :purpose and t.activity = :activity and t.level = :level "
            + "and t.proctorLanguage = :lang and t.status = :status order by t.id desc")
    List<ProctorText> findBySlot(@Param("purpose") ProctorPurpose purpose,
                                 @Param("activity") ProctorActivity activity,
                                 @Param("level") int level,
                                 @Param("lang") String proctorLanguage,
                                 @Param("status") ProctorStatus status);

    @Query("select t from ProctorText t order by t.level, t.purpose, t.activity, t.id desc")
    List<ProctorText> findAllForReview();
}
