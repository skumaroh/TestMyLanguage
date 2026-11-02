package com.infotech.lang.application.video;

import com.infotech.lang.application.assessment.AssessmentMediaService;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The clip library: finds files you uploaded to the clips folder, registers them as DRAFT, lets Meta watch them in the background, and
 * handles an admin's decisions (edit, verify, retire). Only VERIFIED clips are used in a test.
 */
@Service
public class VideoLibrary {

    private static final Logger logger = LoggerFactory.getLogger(VideoLibrary.class);
    /** Files bigger than this are not sent to the model (the clips are meant to be a few hundred KB). */
    static final long MAX_ANALYZE_BYTES = 8L * 1024 * 1024;

    private final VideoClipRepository clips;
    private final AssessmentMediaService media;
    private final ClipAnalyzer analyzer;
    private final String model;
    private final ExecutorService jobs = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "clip-analyzer");
        t.setDaemon(true);
        return t;
    });

    public VideoLibrary(VideoClipRepository clips, AssessmentMediaService media, ClipAnalyzer analyzer,
                        @Value("${meta.text.model:muse-spark-1.3}") String model) {
        this.clips = clips;
        this.media = media;
        this.analyzer = analyzer;
        this.model = model;
    }

    public List<VideoClip> list(String language) {
        return clips.findByLanguageOrderByIdDesc(language);
    }

    public Optional<VideoClip> find(Long id) {
        return clips.findById(id);
    }

    public long countVerified(String language, int level) {
        return clips.findByLanguageAndLevelAndStatus(language, level, VideoClip.VERIFIED).size();
    }

    /** Files in the clips folder that are not registered yet for this language (videos and pictures only). */
    public List<AssessmentMediaService.BlobFile> unregistered(String language) {
        Set<String> known = new HashSet<>();
        clips.findByLanguageOrderByIdDesc(language).forEach(c -> known.add(c.getBlobName()));
        return media.listClipFiles().stream().filter(f -> kindOf(f.name()) != null && !known.contains(f.name())).toList();
    }

    /** Registers every unregistered file as a DRAFT and returns how many were added. */
    public int registerAll(String language, int level) {
        int added = 0;
        for (AssessmentMediaService.BlobFile f : unregistered(language)) {
            clips.save(new VideoClip(language, level, f.name(), kindOf(f.name()), f.sizeBytes()));
            added++;
        }
        return added;
    }

    /** "VIDEO" for .mp4, "IMAGE" for .png / .jpg / .jpeg, otherwise null (the file is ignored). */
    public static String kindOf(String blobName) {
        String n = blobName.toLowerCase(Locale.ROOT);
        if (n.endsWith(".mp4")) {
            return "VIDEO";
        }
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") ? "IMAGE" : null;
    }

    /** Starts the background job in which Meta watches every DRAFT clip that has no description yet; returns how many were queued. */
    public int analyzeDraftsInBackground(String language) {
        List<VideoClip> todo = clips.findByLanguageOrderByIdDesc(language).stream()
                .filter(c -> VideoClip.DRAFT.equals(c.getStatus()) && (c.getEnglish() == null || c.getEnglish().isBlank())).toList();
        for (VideoClip c : todo) {
            Long id = c.getId();
            jobs.submit(() -> {
                try {
                    analyzeOne(id);
                } catch (RuntimeException e) {
                    logger.warn("Clip analysis failed ({})", e.getClass().getSimpleName());
                }
            });
        }
        return todo.size();
    }

    /** Lets Meta watch one clip and stores its draft. Returns false when the file or the model could not be used. */
    public boolean analyzeOne(Long id) {
        VideoClip clip = clips.findById(id).orElse(null);
        if (clip == null) {
            return false;
        }
        if (clip.getSizeBytes() != null && clip.getSizeBytes() > MAX_ANALYZE_BYTES) {
            clip.setMetaClear("NO");
            clip.setMetaProblems("The file is larger than 8 MB: compress it before using it.");
            clips.save(clip);
            return false;
        }
        Optional<byte[]> bytes = media.read(AssessmentMediaService.VIDEO_CONTAINER, clip.getBlobName());
        if (bytes.isEmpty()) {
            return false;
        }
        Optional<ClipDraft> draft = analyzer.analyze(clip.getLanguage(), bytes.get(), clip.isVideo());
        if (draft.isEmpty()) {
            return false;
        }
        ClipDraft d = draft.get();
        clip.setEnglish(d.english());
        clip.setExpectedText(d.expectedText());
        clip.setVariants(String.join("; ", d.variants()));
        clip.setMetaClear(d.clear() ? "YES" : "NO");
        clip.setMetaProblems(d.problems());
        clip.setDifficulty(d.difficulty());
        clip.setModel(model);
        clip.setPromptVersion(ClipAnalyzer.PROMPT_VERSION);
        clips.save(clip);
        return true;
    }

    public void verify(Long id, UUID by) {
        VideoClip clip = clips.findById(id).orElseThrow(() -> new IllegalArgumentException("Clip not found."));
        if (VideoClip.RETIRED.equals(clip.getStatus())) {
            throw new IllegalStateException("A retired clip cannot be verified.");
        }
        if (!clip.hasAnswer()) {
            throw new IllegalStateException("The clip needs a description and an expected answer before it can be verified.");
        }
        clip.markVerified(by);
        clips.save(clip);
    }

    /** Verifies every DRAFT whose description and answer are filled in and that Meta judged clear (YES). Returns how many. */
    public int verifyAllClear(String language, UUID by) {
        int n = 0;
        for (VideoClip c : clips.findByLanguageOrderByIdDesc(language)) {
            if (VideoClip.DRAFT.equals(c.getStatus()) && c.hasAnswer() && "YES".equals(c.getMetaClear())) {
                c.markVerified(by);
                clips.save(c);
                n++;
            }
        }
        return n;
    }

    public void retire(Long id) {
        VideoClip clip = clips.findById(id).orElseThrow(() -> new IllegalArgumentException("Clip not found."));
        clip.setStatus(VideoClip.RETIRED);
        clips.save(clip);
    }

    /** Saves an admin's edits. Editing a verified clip sends it back to draft until it is verified again. */
    public void saveEdits(VideoClip edited) {
        if (VideoClip.VERIFIED.equals(edited.getStatus())) {
            edited.setStatus(VideoClip.DRAFT);
        }
        clips.save(edited);
    }

    public void markUsed(Long id) {
        clips.findById(id).ifPresent(c -> {
            c.markUsed();
            clips.save(c);
        });
    }
}
