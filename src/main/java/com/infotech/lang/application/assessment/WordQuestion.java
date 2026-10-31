package com.infotech.lang.application.assessment;

import java.util.List;

/** One word to build: the prompt shown, the word to build, the letter tiles, and the optional picture and audio (saved blob URLs). */
public record WordQuestion(long id, String prompt, String answer, List<String> tiles, String imageUrl, String audioUrl) {
}
