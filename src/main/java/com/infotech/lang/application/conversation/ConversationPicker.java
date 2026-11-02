package com.infotech.lang.application.conversation;

import com.infotech.lang.application.assessment.AssessAnswerRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Chooses the one conversation of a test. A learner never gets the same conversation twice: verified conversations they have not had
 * come first. If none is left, a new draft is written live and used (flagged UNVERIFIED in the record); if that fails too, the
 * least recently used verified conversation is repeated (flagged REUSED).
 */
@Service
public class ConversationPicker {

    public static final String UNVERIFIED = "UNVERIFIED";
    public static final String REUSED = "REUSED";

    /** The conversation, its eight objectives in order, and a flag ({@code ""}, UNVERIFIED or REUSED). */
    public record Picked(Conversation conversation, List<ConversationObjective> objectives, String flag) {
    }

    private final ConversationRepository conversations;
    private final ConversationLibrary library;
    private final AssessAnswerRepository answers;

    public ConversationPicker(ConversationRepository conversations, ConversationLibrary library, AssessAnswerRepository answers) {
        this.conversations = conversations;
        this.library = library;
        this.answers = answers;
    }

    public Optional<Picked> pick(UUID userId, String language, int level) {
        Set<Long> had = idsHad(answers.findConversationRefsByUser(userId));
        List<Conversation> verified = conversations.findByLanguageAndLevelAndStatus(language, level, Conversation.VERIFIED);
        Optional<Conversation> fresh = chooseUnused(verified, had);
        if (fresh.isPresent()) {
            return complete(fresh.get(), "");
        }
        Conversation live = library.generateOne(language, level);
        if (live != null) {
            return complete(live, UNVERIFIED);
        }
        return chooseLeastRecentlyUsed(verified).flatMap(c -> complete(c, REUSED));
    }

    public static final String ADMIN_TEST = "ADMIN_TEST";

    /**
     * For an admin trying the block: the conversation with the given id (a draft may be previewed; a retired one may not), or
     * any verified conversation at random when {@code conversationId} is null. History is ignored and the use count is not
     * changed, so testing does not use up the library.
     */
    public Optional<Picked> pickForAdmin(String language, int level, Long conversationId) {
        Conversation chosen;
        if (conversationId != null) {
            chosen = conversations.findById(conversationId)
                    .filter(c -> language.equalsIgnoreCase(c.getLanguage()) && !Conversation.RETIRED.equals(c.getStatus()))
                    .orElse(null);
        } else {
            List<Conversation> verified = new ArrayList<>(
                    conversations.findByLanguageAndLevelAndStatus(language, level, Conversation.VERIFIED));
            Collections.shuffle(verified);
            chosen = verified.isEmpty() ? null : verified.get(0);
        }
        if (chosen == null) {
            return Optional.empty();
        }
        List<ConversationObjective> objectives = library.objectivesOf(chosen.getId());
        return objectives.size() == ConversationGenerator.OBJECTIVES
                ? Optional.of(new Picked(chosen, objectives, ADMIN_TEST)) : Optional.empty();
    }

    private Optional<Picked> complete(Conversation c, String flag) {
        List<ConversationObjective> objectives = library.objectivesOf(c.getId());
        if (objectives.size() != ConversationGenerator.OBJECTIVES) {
            return Optional.empty();
        }
        library.markUsed(c.getId());
        return Optional.of(new Picked(c, objectives, flag));
    }

    /** The conversation ids in references such as {@code conv:12:3}. */
    static Set<Long> idsHad(List<String> refs) {
        Set<Long> ids = new HashSet<>();
        for (String ref : refs) {
            String[] parts = ref.split(":");
            if (parts.length >= 2 && "conv".equals(parts[0])) {
                try {
                    ids.add(Long.parseLong(parts[1]));
                } catch (NumberFormatException e) {
                    // not a conversation reference of ours
                }
            }
        }
        return ids;
    }

    static Optional<Conversation> chooseUnused(List<Conversation> verified, Set<Long> had) {
        List<Conversation> unused = new ArrayList<>();
        for (Conversation c : verified) {
            if (!had.contains(c.getId())) {
                unused.add(c);
            }
        }
        if (unused.isEmpty()) {
            return Optional.empty();
        }
        Collections.shuffle(unused);
        return Optional.of(unused.get(0));
    }

    static Optional<Conversation> chooseLeastRecentlyUsed(List<Conversation> verified) {
        return verified.stream().min((a, b) -> {
            if (a.getLastUsedAt() == null || b.getLastUsedAt() == null) {
                return a.getLastUsedAt() == null ? (b.getLastUsedAt() == null ? 0 : -1) : 1;
            }
            return a.getLastUsedAt().compareTo(b.getLastUsedAt());
        });
    }
}
