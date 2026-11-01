package com.infotech.lang.application.conversation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationObjectiveRepository extends JpaRepository<ConversationObjective, Long> {

    List<ConversationObjective> findByConversationIdOrderByPosition(Long conversationId);
}
