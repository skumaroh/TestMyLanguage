package com.infotech.lang.application.video;

import com.infotech.lang.application.assessment.AssessAnswerRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Chooses the clips of one test. A learner is shown clips they have not seen before first; if there are not enough of those, the ones seen
 * longest ago fill the rest. A clip never appears twice in one test.
 */
@Service
public class VideoPicker {

    /** A test needs this many clips to be offered at all (4, and 4 more when under 75%). */
    public static final int NEEDED = 8;

    private final VideoClipRepository clips;
    private final AssessAnswerRepository answers;

    public VideoPicker(VideoClipRepository clips, AssessAnswerRepository answers) {
        this.clips = clips;
        this.answers = answers;
    }

    /** True when there are enough verified clips for a full video block. */
    public boolean isAvailable(String language, int level) {
        return clips.findByLanguageAndLevelAndStatus(language, level, VideoClip.VERIFIED).size() >= NEEDED;
    }

    public List<VideoClip> pick(UUID userId, String language, int level, int count) {
        Set<Long> had = idsHad(answers.findClipRefsByUser(userId));
        return choose(clips.findByLanguageAndLevelAndStatus(language, level, VideoClip.VERIFIED), had, count);
    }

    /** For an admin trying the block: any verified clips at random, history ignored. */
    public List<VideoClip> pickForAdmin(String language, int level, int count) {
        List<VideoClip> all = new ArrayList<>(clips.findByLanguageAndLevelAndStatus(language, level, VideoClip.VERIFIED));
        Collections.shuffle(all);
        return all.subList(0, Math.min(count, all.size()));
    }

    /** The clip ids in references such as {@code clip:12}. */
    static Set<Long> idsHad(List<String> refs) {
        Set<Long> ids = new HashSet<>();
        for (String ref : refs) {
            String[] parts = ref.split(":");
            if (parts.length >= 2 && "clip".equals(parts[0])) {
                try {
                    ids.add(Long.parseLong(parts[1]));
                } catch (NumberFormatException e) {
                    // not a clip reference of ours
                }
            }
        }
        return ids;
    }

    /** Unseen clips first (random order), then the ones used longest ago; at most {@code count}, never the same clip twice. */
    static List<VideoClip> choose(List<VideoClip> verified, Set<Long> had, int count) {
        List<VideoClip> unseen = new ArrayList<>();
        List<VideoClip> seen = new ArrayList<>();
        for (VideoClip c : verified) {
            (had.contains(c.getId()) ? seen : unseen).add(c);
        }
        Collections.shuffle(unseen);
        seen.sort(Comparator.comparing(VideoClip::getLastUsedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        List<VideoClip> picked = new ArrayList<>(unseen);
        picked.addAll(seen);
        return new ArrayList<>(picked.subList(0, Math.min(count, picked.size())));
    }
}
