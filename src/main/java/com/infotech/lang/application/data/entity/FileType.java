package com.infotech.lang.application.data.entity;

/**
 * Enum representing the type of media file being uploaded to Azure Blob Storage.
 * Used for organizing files into separate containers and generating appropriate blob paths.
 * 
 * @see com.infotech.lang.application.data.service.AzureBlobService
 */
public enum FileType {
    /**
     * Image files (JPEG, PNG, GIF, etc.)
     * Stored in language-tool-images container
     */
    IMAGE("images"),
    
    /**
     * Audio files (MP3, WAV, OGG, etc.)
     * Stored in language-tool-audio container
     */
    AUDIO("audio"),
    
    /**
     * Video files (MP4, WebM, etc.)
     * Stored in language-tool-videos container
     */
    VIDEO("videos"),

    /**
     * Handwriting PNG submissions captured from the WritingSkill module.
     * Stored in language-tool-handwriting container.
     */
    HANDWRITING("handwriting"),

    /**
     * Admin-provided reference letter images used to score handwriting submissions.
     * Stored in language-tool-letter-references container, per language (letter glyphs
     * differ between Tamil and Hindi even for the "same" sound).
     */
    LETTER_REFERENCE("letter-references");
    
    private final String containerSuffix;
    
    FileType(String containerSuffix) {
        this.containerSuffix = containerSuffix;
    }
    
    /**
     * Returns the container suffix for this file type.
     * Used to construct the full container name: "language-tool-{suffix}"
     * 
     * @return container suffix (e.g., "images", "audio", "videos")
     */
    public String getContainerSuffix() {
        return containerSuffix;
    }
    
    /**
     * Returns the full Azure Blob container name for this file type.
     * 
     * @return container name (e.g., "language-tool-images")
     */
    public String getContainerName() {
        return "language-tool-" + containerSuffix;
    }
}
