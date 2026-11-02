package com.infotech.lang.application.video;

import java.util.List;

/** What the model wrote after watching one clip, before an admin reviews it. */
public record ClipDraft(String english, String expectedText, List<String> variants, boolean clear, String problems, String difficulty) {
}
