package com.infotech.lang.application.report;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the answer log line that each block writes into {@code assess_answer.answer_given} and turns it into plain facts for the
 * report: what the student answered, the verdict and reason, the spelling note, and how the answer was made (spoken, with help...).
 * Words and audio words store just the built word; letters store the shape comparison; translation and conversation store markers.
 */
public final class AnswerLog {

    /** What a log line says. {@code answer} is empty for skipped questions and drawings. */
    public record Parsed(String answer, String verdict, String reason, String spellingCorrected, String asked, String next,
                         boolean skipped, String skipReason, List<String> notes) {
    }

    private static final Pattern SPELLING = Pattern.compile("\\s*\\{SPELLING ([A-Z_]+)(?:: ([^}]*))?\\}");
    private static final Pattern VERDICT = Pattern.compile("\\s*\\[([A-Z_]+)(?:: (.*))?\\]\\s*$");
    private static final Pattern SKIPPED = Pattern.compile("^\\[SKIPPED: ([A-Z]+)\\]$");
    private static final Pattern BEST = Pattern.compile("best=(\\S+)");
    private static final Pattern HELP = Pattern.compile("^SPELLING-HELP (\\d+) ");
    private static final String[] FLAGS = {"FLAG grader unavailable: ", "FLAG UNVERIFIED conversation: ", "FLAG REUSED conversation: ",
            "FLAG ADMIN_TEST conversation: ", "FLAG unclear: "};

    private AnswerLog() {
    }

    public static Parsed parse(String log) {
        List<String> notes = new ArrayList<>();
        String text = log == null ? "" : log.trim();
        Matcher skipped = SKIPPED.matcher(text);
        if (skipped.matches()) {
            return new Parsed("", "", "", "", "", "", true, skipped.group(1), notes);
        }
        String asked = "";
        String next = "";
        int nextAt = text.indexOf(" | NEXT: ");
        if (nextAt >= 0) {
            next = text.substring(nextAt + " | NEXT: ".length()).trim();
            text = text.substring(0, nextAt);
        }
        int askedAt = text.indexOf(" | ASKED: ");
        if (askedAt >= 0) {
            asked = text.substring(askedAt + " | ASKED: ".length()).trim();
            text = text.substring(0, askedAt);
        }
        String spellingCorrected = "";
        Matcher spelling = SPELLING.matcher(text);
        if (spelling.find()) {
            spellingCorrected = spelling.group(2) == null ? "" : spelling.group(2).trim();
            notes.add(spelling.group(1).equals("MANY_ERRORS") ? "Many spelling slips" : "A few spelling slips");
            text = spelling.replaceFirst("");
        }
        String verdict = "";
        String reason = "";
        Matcher v = VERDICT.matcher(text);
        if (v.find()) {
            verdict = v.group(1);
            reason = v.group(2) == null ? "" : v.group(2).trim();
            text = text.substring(0, v.start());
        }
        text = text.trim();
        boolean again = true;
        while (again) {
            again = false;
            for (String flag : FLAGS) {
                if (text.startsWith(flag)) {
                    notes.add(flag.startsWith("FLAG grader") ? "The checker could not be reached for this answer"
                            : flag.startsWith("FLAG unclear") ? "The drawing was unclear"
                            : "This conversation was " + flag.split(" ")[1].toLowerCase());
                    text = text.substring(flag.length());
                    again = true;
                }
            }
            if (text.startsWith("TILES ")) {
                notes.add("Word tiles");
                text = text.substring(6);
                again = true;
            } else if (text.startsWith("VOICE-EDITED ")) {
                notes.add("Spoken, then edited");
                text = text.substring(13);
                again = true;
            } else if (text.startsWith("VOICE ")) {
                notes.add("Spoken");
                text = text.substring(6);
                again = true;
            } else if (text.startsWith("HINT ")) {
                notes.add("Used \"Show English\"");
                text = text.substring(5);
                again = true;
            }
            Matcher help = HELP.matcher(text);
            if (help.find()) {
                notes.add("Used spelling help (" + help.group(1) + ")");
                text = text.substring(help.end());
                again = true;
            }
        }
        Matcher best = BEST.matcher(text);
        if (text.startsWith("judged") && best.find()) {
            notes.add("Your drawing looked most like " + best.group(1));
            text = "";
        } else if (text.startsWith("unjudged")) {
            text = "";
        }
        return new Parsed(text.trim(), verdict, reason, spellingCorrected, asked, next, false, "", notes);
    }
}
