package com.infotech.lang.application.handwriting;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReferenceLetterRepository extends JpaRepository<ReferenceLetter, Long> {

    /** Published rows for a letter, the house (shared) row first. */
    @Query("select r from ReferenceLetter r where r.letter = :letter and r.published = true "
            + "order by case when r.curriculumId is null then 0 else 1 end, r.id")
    List<ReferenceLetter> findForLetter(@Param("letter") String letter);

    /** House (shared) letters of a group, in display order. */
    @Query("select r from ReferenceLetter r where r.category = :category and r.published = true and r.curriculumId is null "
            + "order by r.displayOrder, r.id")
    List<ReferenceLetter> findHouseByCategory(@Param("category") String category);

    @Query("select r from ReferenceLetter r where r.category = :category and r.subCategory = :sub and r.published = true "
            + "and r.curriculumId is null order by r.displayOrder, r.id")
    List<ReferenceLetter> findHouseByCategoryAndSub(@Param("category") String category, @Param("sub") String subCategory);
}
