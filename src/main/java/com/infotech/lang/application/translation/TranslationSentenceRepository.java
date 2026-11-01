package com.infotech.lang.application.translation;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TranslationSentenceRepository extends JpaRepository<TranslationSentence, Long> {

    /** Sentences a person has checked (verified or corrected), the shortest English ones first (simple sentences). */
    @Query("select s from TranslationSentence s where s.reviewStatus in ('VERIFIED', 'CORRECTED') "
            + "and s.naturalTranslation is not null and s.naturalTranslation <> '' "
            + "and s.sentenceText is not null and s.sentenceText <> '' "
            + "order by length(s.naturalTranslation), s.id")
    List<TranslationSentence> findCheckedShortestFirst(Pageable pageable);
}
