package com.infotech.lang.application.conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The conversation library: generation (a background job, one conversation at a time, saved as DRAFT), the admin's decisions
 * (verify, retire, edit) and lookups. Only VERIFIED conversations are used in a test.
 */
@Service
public class ConversationLibrary {

    private static final Logger logger = LoggerFactory.getLogger(ConversationLibrary.class);
    static final int MAX_PER_REQUEST = 5;

    private final ConversationRepository conversations;
    private final ConversationObjectiveRepository objectives;
    private final ConversationGenerator generator;
    private final String model;
    private final ExecutorService jobs = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "conversation-generator");
        t.setDaemon(true);
        return t;
    });

    public ConversationLibrary(ConversationRepository conversations, ConversationObjectiveRepository objectives,
                               ConversationGenerator generator, @Value("${meta.text.model:muse-spark-1.3}") String model) {
        this.conversations = conversations;
        this.objectives = objectives;
        this.generator = generator;
        this.model = model;
    }

    /** Starts the background job that writes up to {@value #MAX_PER_REQUEST} new DRAFT conversations; returns how many were requested. */
    public int generateInBackground(String language, int level, int count) {
        int n = Math.max(1, Math.min(count, MAX_PER_REQUEST));
        for (int i = 0; i < n; i++) {
            jobs.submit(() -> {
                try {
                    generateOne(language, level);
                } catch (RuntimeException e) {
                    logger.warn("Conversation generation failed ({})", e.getClass().getSimpleName());
                }
            });
        }
        return n;
    }

    /** Writes one DRAFT conversation now. Returns it, or null when the model could not be reached or its answer failed the checks. */
    public Conversation generateOne(String language, int level) {
        List<String> titles = new ArrayList<>();
        conversations.findByLanguageOrderByIdDesc(language).forEach(c -> titles.add(c.getScenarioTitle()));
        return generator.generate(language, level, titles).map(g -> save(language, level, g)).orElse(null);
    }

    @Transactional
    public Conversation save(String language, int level, GeneratedConversation g) {
        Conversation c = new Conversation(language, level, "META", g.scenarioTitle(), g.personaName());
        c.setPersonaDescription(g.personaDescription());
        c.setSceneNote(g.sceneNote());
        c.setIntroLine(g.introLine());
        c.setIntroEnglish(g.introEnglish());
        c.setClosingLine(g.closingLine());
        c.setClosingEnglish(g.closingEnglish());
        c.setDifficulty(g.difficulty());
        c.setModel(model);
        c.setPromptVersion(ConversationGenerator.PROMPT_VERSION);
        c = conversations.save(c);
        int position = 1;
        for (GeneratedConversation.Objective o : g.objectives()) {
            objectives.save(new ConversationObjective(c.getId(), position++, o.objectiveEn(), o.vocabularyHint(), o.fallbackLine()));
        }
        return c;
    }

    @Transactional(readOnly = true)
    public List<Conversation> list(String language) {
        return conversations.findByLanguageOrderByIdDesc(language);
    }

    @Transactional(readOnly = true)
    public List<ConversationObjective> objectivesOf(Long conversationId) {
        return objectives.findByConversationIdOrderByPosition(conversationId);
    }

    @Transactional
    public void verify(Long id, UUID by) {
        Conversation c = conversations.findById(id).orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
        if (Conversation.RETIRED.equals(c.getStatus())) {
            throw new IllegalStateException("A retired conversation cannot be verified.");
        }
        if (objectives.findByConversationIdOrderByPosition(id).size() != ConversationGenerator.OBJECTIVES) {
            throw new IllegalStateException("This conversation does not have all " + ConversationGenerator.OBJECTIVES
                    + " objectives, so it cannot be verified. Retire it and write a new one.");
        }
        c.markVerified(by);
        conversations.save(c);
    }

    @Transactional
    public void retire(Long id) {
        Conversation c = conversations.findById(id).orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
        c.setStatus(Conversation.RETIRED);
        conversations.save(c);
    }

    /** Saves an admin's edits to the conversation and its objectives. Editing a VERIFIED conversation sends it back to DRAFT. */
    @Transactional
    public void saveEdits(Conversation edited, List<ConversationObjective> editedObjectives) {
        if (Conversation.VERIFIED.equals(edited.getStatus())) {
            edited.setStatus(Conversation.DRAFT);
        }
        conversations.save(edited);
        objectives.saveAll(editedObjectives);
    }
}
