package com.infotech.lang.application.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WordPictureNamingTest {

    @Test
    void aNewPictureIsAlwaysTheNewestOfItsWord() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 5, 10, 0, 0);
        String made = AssessmentMediaService.newPictureName("Ice Cream", "png", now);
        assertTrue(made.matches("images/\\d{19}_ice_cream\\.png"), made);
        assertTrue(AssessmentMediaService.madeByAdminPage(made));
        assertFalse(AssessmentMediaService.madeByAdminPage("images/20260604_dog.jpg"));
        Map<String, String> index = AssessmentMediaService.indexImages(List.of("images/tree.png", "images/20260507_ab12cd34_tree.png",
                "images/1730000000000_tree.jpg", made.replace("ice_cream", "tree")));
        assertEquals(made.replace("ice_cream", "tree"), index.get("tree"));
        // a bare name must not beat a dated one
        assertEquals("images/20260604_dog.jpg", AssessmentMediaService.indexImages(List.of("images/dog.png", "images/20260604_dog.jpg")).get("dog"));
    }

    @Test
    void picturesOfAWordAreListedNewestFirst() {
        Map<String, List<String>> byWord = AssessmentMediaService.picturesByWord(List.of("images/20260101_dog.jpg", "images/dog.png",
                "images/20260901_dog.png", "images/notes.txt"));
        assertEquals(List.of("images/20260901_dog.png", "images/20260101_dog.jpg", "images/dog.png"), byWord.get("dog"));
        assertFalse(byWord.containsKey("notes"));
    }
}
