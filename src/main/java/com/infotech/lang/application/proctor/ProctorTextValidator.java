package com.infotech.lang.application.proctor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Checks a generated or edited text before a person sees it. A text is the English part, a newline, then a short
 * phrase in the tested language. Returns the problems found; an empty list means the text passed.
 */
@Component
public class ProctorTextValidator {

    public static final int MAX_BODY_CHARS = 600;
    private static final int MAX_ENGLISH_WORDS = 60;

    private static final Pattern LINK = Pattern.compile("(?i)(https?://|www\\.|\\S+@\\S+\\.\\S+)");
    private static final Pattern BANNED = Pattern.compile(
            "(?i)\\b(scores?|scored|correct|incorrect|wrong|pass|passed|passing|fail|failed|failing|marks?|grades?|graded)\\b");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]*)}");

    public List<String> validate(String body, String testedLanguage) {
        List<String> problems = new ArrayList<>();
        if (body == null || body.isBlank()) {
            problems.add("empty text");
            return problems;
        }
        if (body.length() > MAX_BODY_CHARS) {
            problems.add("longer than " + MAX_BODY_CHARS + " characters");
        }
        if (LINK.matcher(body).find()) {
            problems.add("contains a link or an email address");
        }
        Matcher placeholders = PLACEHOLDER.matcher(body);
        while (placeholders.find()) {
            if (!"name".equals(placeholders.group(1))) {
                problems.add("unknown placeholder {" + placeholders.group(1) + "}");
            }
        }
        if (body.indexOf('`') >= 0 || body.contains("**") || body.contains("##")) {
            problems.add("contains markdown");
        }

        int newline = body.indexOf('\n');
        String english = newline < 0 ? body : body.substring(0, newline);
        String phrase = newline < 0 ? "" : body.substring(newline + 1);

        if (BANNED.matcher(english).find()) {
            problems.add("mentions scores, results or right and wrong answers");
        }
        if (english.trim().split("\\s+").length > MAX_ENGLISH_WORDS) {
            problems.add("English part is longer than " + MAX_ENGLISH_WORDS + " words");
        }
        if (scriptShare(english, Character.UnicodeScript.LATIN) < 0.8 || letterCount(english) < 20) {
            problems.add("English part is not English text");
        }
        Character.UnicodeScript target = scriptFor(testedLanguage);
        if (target != null && (letterCount(phrase) < 3 || scriptShare(phrase, target) < 0.6)) {
            problems.add("the phrase is not written in " + testedLanguage + " script");
        }
        return problems;
    }

    static Character.UnicodeScript scriptFor(String testedLanguage) {
        if ("tamil".equalsIgnoreCase(testedLanguage)) {
            return Character.UnicodeScript.TAMIL;
        }
        if ("hindi".equalsIgnoreCase(testedLanguage)) {
            return Character.UnicodeScript.DEVANAGARI;
        }
        return null;
    }

    private static int letterCount(String text) {
        return (int) text.codePoints().filter(Character::isLetter).count();
    }

    private static double scriptShare(String text, Character.UnicodeScript script) {
        long letters = text.codePoints().filter(Character::isLetter).count();
        if (letters == 0) {
            return 0;
        }
        long inScript = text.codePoints()
                .filter(Character::isLetter)
                .filter(cp -> Character.UnicodeScript.of(cp) == script)
                .count();
        return (double) inScript / letters;
    }
}
