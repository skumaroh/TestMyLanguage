package com.infotech.lang.application.conversation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByLanguageOrderByIdDesc(String language);

    List<Conversation> findByLanguageAndLevelAndStatus(String language, int level, String status);
}
