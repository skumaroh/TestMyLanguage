package com.infotech.lang.application.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.infotech.lang.application.assessment.AssessAnswer;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersonaLineReviewTest {

    private static AssessAnswer row(int position, String log) {
        AssessAnswer a = new AssessAnswer();
        a.setPosition(position);
        a.setAnswerGiven(log);
        return a;
    }

    @Test
    void groupsTheSameLineAndSkipsRowsWithoutOne() {
        List<PersonaLineReview.LiveLine> lines = PersonaLineReview.group(List.of(
                row(1, "என் பெயர் குமார் [MET: ok] | ASKED: உங்கள் பெயர் என்ன? | NEXT: சரி, குமார்! என்ன வேண்டும்?"),
                row(1, "குமார் [MET: ok] | ASKED: உங்கள் பெயர் என்ன? | NEXT: சரி, குமார்! என்ன வேண்டும்?"),
                row(2, "[SKIPPED: DONE]"),
                row(3, "x [MET: ok] | ASKED: எத்தனை? | NEXT: ")));
        assertEquals(1, lines.size());
        assertEquals(2, lines.get(0).times());
        assertEquals("உங்கள் பெயர் என்ன?", lines.get(0).asked());
    }
}
