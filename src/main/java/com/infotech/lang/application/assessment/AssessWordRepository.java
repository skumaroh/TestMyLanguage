package com.infotech.lang.application.assessment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessWordRepository extends JpaRepository<AssessWord, Long> {

    /** Published words of a level in the house (default) curriculum. */
    @Query("select w from AssessWord w where w.level = :level and w.curriculumId is null "
            + "and w.assetStatus = 'PUBLISHED' and w.assetType = 'WORD' order by w.id")
    List<AssessWord> findHouseWords(@Param("level") int level);
}
