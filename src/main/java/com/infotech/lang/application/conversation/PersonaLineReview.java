package com.infotech.lang.application.conversation;

import com.infotech.lang.application.assessment.AssessAnswer;
import com.infotech.lang.application.assessment.AssessAnswerRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets an admin see the lines the persona actually said in real attempts. Nothing new is stored: every turn already saves the persona
 * line it answered ({@code ASKED:}) and the line Meta wrote next ({@code NEXT:}) in the answer log. This only reads and groups them.
 */
@Service
public class PersonaLineReview {

    static final int MAX_LINES = 40;

    /** A distinct line Meta wrote after the given goal, and how many times it appeared. */
    public record LiveLine(int position, String asked, String next, int times) {
    }

    private final AssessAnswerRepository answers;

    public PersonaLineReview(AssessAnswerRepository answers) {
        this.answers = answers;
    }

    @Transactional(readOnly = true)
    public List<LiveLine> liveLines(Long conversationId) {
        return group(answers.findTop300ByItemRefStartingWithOrderByIdDesc("conv:" + conversationId + ":"));
    }

    static List<LiveLine> group(List<AssessAnswer> rows) {
        Map<String, LiveLine> seen = new LinkedHashMap<>();
        for (AssessAnswer a : rows) {
            String log = a.getAnswerGiven() == null ? "" : a.getAnswerGiven();
            int n = log.lastIndexOf(" | NEXT: ");
            if (n < 0) {
                continue;
            }
            String next = log.substring(n + " | NEXT: ".length()).trim();
            String head = log.substring(0, n);
            int k = head.lastIndexOf(" | ASKED: ");
            String asked = k < 0 ? "" : head.substring(k + " | ASKED: ".length()).trim();
            if (next.isEmpty()) {
                continue;
            }
            String key = a.getPosition() + "|" + asked + "|" + next;
            LiveLine old = seen.get(key);
            seen.put(key, old == null ? new LiveLine(a.getPosition(), asked, next, 1)
                    : new LiveLine(old.position(), old.asked(), old.next(), old.times() + 1));
        }
        List<LiveLine> out = new ArrayList<>(seen.values());
        return out.size() > MAX_LINES ? out.subList(0, MAX_LINES) : out;
    }
}
