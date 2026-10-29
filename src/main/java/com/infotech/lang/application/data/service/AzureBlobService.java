package com.infotech.lang.application.data.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.infotech.lang.application.data.entity.FileType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing file uploads and deletions in Azure Blob Storage.
 * Handles multimedia assets (images, audio, video) for the LanguageTool application.
 * 
 * Files are organized with a hybrid structure:
 * - Images (shared across languages): images/{timestamp}_{filename}
 * - Audio (language-specific): audio/{language}/{timestamp}_{filename}
 * - Video (language-specific): video/{language}/{timestamp}_{filename}
 * 
 * @author Kumar Saravanan
 * @since 1.0.0
 */
@Service
public class AzureBlobService {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureBlobService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    @Value("${azure.storage.connection-string:}")
    private String connectionString;
    
    private BlobServiceClient blobServiceClient;

    @PostConstruct
    void logConfigurationStatus() {
        if (isConfigured()) {
            logger.info("Azure Blob Storage is configured for local media loading.");
            return;
        }

        logger.warn(getMissingConfigurationMessage());
    }

    public boolean isConfigured() {
        return connectionString != null && !connectionString.isBlank();
    }

    public String getMissingConfigurationMessage() {
        return "Azure Blob Storage is not configured. Local image/audio loading requires "
            + "AZURE_STORAGE_CONNECTION_STRING in .env or azure.storage.connection-string "
            + "in application.properties.";
    }
    
    /**
     * Initializes the BlobServiceClient lazily when first needed.
     * Uses connection string from environment variable or application.properties.
     * 
     * @return initialized BlobServiceClient
     * @throws IllegalStateException if connection string is not configured
     */
    private BlobServiceClient getBlobServiceClient() {
        if (blobServiceClient == null) {
            if (!isConfigured()) {
                throw new IllegalStateException(getMissingConfigurationMessage());
            }
            blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
            logger.info("Azure BlobServiceClient initialized successfully");
        }
        return blobServiceClient;
    }
    
    /**
     * Uploads a file to Azure Blob Storage and returns the public URL.
     *
     * File naming patterns — see {@link #generateBlobName}'s own javadoc for why {@code
     * uniqueSuffix} is there:
     * - Images (shared): images/{timestamp}_{uniqueSuffix}_{sanitizedFilename}
     * - Audio (language-specific): audio/{language}/{timestamp}_{uniqueSuffix}_{sanitizedFilename}
     * - Video (language-specific): video/{language}/{timestamp}_{uniqueSuffix}_{sanitizedFilename}
     *
     * @param content    InputStream of the file content
     * @param fileName   Original filename (will be sanitized)
     * @param fileType   Type of file (IMAGE, AUDIO, VIDEO)
     * @param language   Language code (e.g., "tamil", "hindi") - used for audio/video only
     * @return Public URL of the uploaded blob
     * @throws IllegalArgumentException if parameters are invalid
     * @throws BlobStorageException if upload fails
     */
    public String uploadFile(InputStream content, String fileName, FileType fileType, String language) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (fileType == null) {
            throw new IllegalArgumentException("File type cannot be null");
        }
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("Language cannot be null or empty");
        }
        
        try {
            // Generate blob name with timestamp and sanitized filename
            String blobName = generateBlobName(fileName, fileType, language);
            
            // Get or create container for this file type
            String containerName = fileType.getContainerName();
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            
            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created new container: {}", containerName);
            }
            
            // Upload blob
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(content, true); // overwrite if exists
            
            String blobUrl = blobClient.getBlobUrl();
            logger.info("Successfully uploaded file: {} to container: {}", blobName, containerName);
            
            return blobUrl;
            
        } catch (BlobStorageException e) {
            logger.error("Failed to upload file {} of type {} for language {}: {}", 
                fileName, fileType, language, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error uploading file {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to Azure Blob Storage", e);
        }
    }
    
    /**
     * Downloads a file from Azure Blob Storage and returns the content as a byte array.
     * 
     * @param containerName Name of the container (e.g., "language-tool-images-container")
     * @param blobName      Name of the blob/file to download (e.g., "apple.png")
     * @return Byte array containing the file content
     * @throws IllegalArgumentException if parameters are invalid
     * @throws BlobStorageException if download fails (e.g., 404 if blob not found)
     */
    public byte[] downloadFile(String containerName, String blobName) {
        if (containerName == null || containerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Container name cannot be null or empty");
        }
        if (blobName == null || blobName.trim().isEmpty()) {
            throw new IllegalArgumentException("Blob name cannot be null or empty");
        }
        
        try {
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            byte[] content = blobClient.downloadContent().toBytes();
            logger.info("Successfully downloaded blob: {} from container: {}", blobName, containerName);
            return content;
            
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                logger.warn("Blob not found: {} in container: {}", blobName, containerName);
            } else {
                logger.error("Failed to download blob {} from container {}: {}", 
                    blobName, containerName, e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error downloading blob {} from container {}: {}", 
                blobName, containerName, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from Azure Blob Storage", e);
        }
    }
    
    /**
     * Downloads a file from Azure Blob Storage given its full blob URL.
     * Extracts container name and blob name from the URL, the same way {@link #deleteFile(String)} does.
     *
     * @param blobUrl Full URL of the blob to download
     * @return Byte array containing the file content
     * @throws IllegalArgumentException if the URL is invalid or cannot be parsed
     * @throws BlobStorageException if download fails (e.g., 404 if blob not found)
     */
    public byte[] downloadFile(String blobUrl) {
        if (blobUrl == null || blobUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Blob URL cannot be null or empty");
        }

        String[] parts = blobUrl.split("/", 5);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid blob URL format: " + blobUrl);
        }

        String containerName = parts[3];
        String blobName = java.net.URLDecoder.decode(parts[4], java.nio.charset.StandardCharsets.UTF_8);
        return downloadFile(containerName, blobName);
    }

    /**
     * Deletes a file from Azure Blob Storage using its URL.
     * Extracts container name and blob name from the URL and performs deletion.
     * 
     * URL format: https://{account}.blob.core.windows.net/{container}/{blobName}
     * Example: https://ltstorageaccount101.blob.core.windows.net/language-tool-images/tamil/images/20260505120000_dog.png
     * 
     * @param blobUrl Full URL of the blob to delete
     * @throws IllegalArgumentException if URL is invalid or cannot be parsed
     * @throws BlobStorageException if deletion fails
     */
    public void deleteFile(String blobUrl) {
        if (blobUrl == null || blobUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Blob URL cannot be null or empty");
        }
        
        try {
            // Parse URL to extract container and blob name
            // URL format: https://{account}.blob.core.windows.net/{container}/{blobName}
            String[] parts = blobUrl.split("/", 5);
            if (parts.length < 5) {
                throw new IllegalArgumentException("Invalid blob URL format: " + blobUrl);
            }
            
            String containerName = parts[3]; // Container name after domain
            String blobName = parts[4];      // Blob name (everything after container)
            
            // Get blob client and delete
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (blobClient.exists()) {
                blobClient.delete();
                logger.info("Successfully deleted blob: {} from container: {}", blobName, containerName);
            } else {
                logger.warn("Blob not found for deletion: {} in container: {}", blobName, containerName);
            }
            
        } catch (BlobStorageException e) {
            logger.error("Failed to delete blob {}: {}", blobUrl, e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid blob URL: {}", blobUrl);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error deleting blob {}: {}", blobUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from Azure Blob Storage", e);
        }
    }
    
    /**
     * Deletes a blob directly by container name and blob name (path within container).
     *
     * @param containerName Azure Blob container name
     * @param blobName      Blob path within the container
     */
    public void deleteBlobByName(String containerName, String blobName) {
        try {
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            if (blobClient.exists()) {
                blobClient.delete();
                logger.info("Deleted blob {} from container {}", blobName, containerName);
            } else {
                logger.warn("Blob not found for deletion: {} in container {}", blobName, containerName);
            }
        } catch (Exception e) {
            logger.error("Failed to delete blob {} from {}: {}", blobName, containerName, e.getMessage());
            throw new RuntimeException("Failed to delete blob from Azure Blob Storage", e);
        }
    }

    /**
     * Checks whether a specific blob exists in the given container.
     * Returns {@code false} (not {@code null}) when Azure is not configured or
     * if any error occurs — this allows callers to fall back gracefully.
     */
    public boolean blobExists(String containerName, String blobName) {
        if (!isConfigured()) return false;
        try {
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            return containerClient.getBlobClient(blobName).exists();
        } catch (Exception e) {
            logger.debug("blobExists check failed for {}/{}: {}", containerName, blobName, e.getMessage());
            return false;
        }
    }

    /**
     * Uploads raw bytes to a specific blob name (path within container).
     * Returns the public URL of the uploaded blob.
     *
     * @param containerName Azure Blob container name
     * @param blobName      Desired blob path within the container
     * @param bytes         Raw file bytes
     * @param contentType   MIME type (informational only; not set as metadata)
     * @return Public URL of the uploaded blob
     */
    public String uploadBlobByName(String containerName, String blobName, byte[] bytes, String contentType) {
        try {
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created container {}", containerName);
            }
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(new java.io.ByteArrayInputStream(bytes), bytes.length, true);
            logger.info("Uploaded blob {} to container {}", blobName, containerName);
            return blobClient.getBlobUrl();
        } catch (Exception e) {
            logger.error("Failed to upload blob {} to {}: {}", blobName, containerName, e.getMessage());
            throw new RuntimeException("Failed to upload blob to Azure Blob Storage", e);
        }
    }

    /**
     * Returns the deterministic public URL for a blob path, without checking existence or
     * performing any network I/O — {@code BlobClient#getBlobUrl()} is pure string construction
     * from the configured account/container/blob name. Used by callers (e.g.
     * {@link ConceptImageService}) that have already confirmed a blob exists (via
     * {@link #blobExists}) and need to reconstruct its URL without re-uploading/re-downloading it.
     *
     * @param containerName Azure Blob container name
     * @param blobName      Blob path within the container
     * @return the blob's public URL, or {@code null} if Azure Storage isn't configured or the URL
     *         cannot be constructed
     */
    public String getBlobUrl(String containerName, String blobName) {
        return buildBlobUrl(containerName, blobName);
    }

    /**
     * Generates a unique blob name with timestamp, a short random disambiguator, and a sanitized
     * filename.
     *
     * <p>Patterns:
     * - Images (shared): images/{timestamp}_{uniqueSuffix}_{sanitizedFilename}
     * - Audio (language-specific): audio/{language}/{timestamp}_{uniqueSuffix}_{sanitizedFilename}
     * - Video (language-specific): video/{language}/{timestamp}_{uniqueSuffix}_{sanitizedFilename}
     *
     * <p><b>Why {@code uniqueSuffix} exists — a real collision, not a hypothetical one.</b>
     * {@link #sanitizeFileName} strips every character outside {@code [a-zA-Z0-9._-]}, which is
     * <i>every</i> Devanagari or Tamil character — so any filename built purely from Hindi/Tamil
     * source text (which is the overwhelming majority of the audio this app synthesizes) sanitizes
     * down to nothing but its extension, e.g. both {@code "आलू कितना रुपया हाय?.mp3"} and
     * {@code "आलू कितने रुपये का है?.mp3"} — two genuinely different sentences — sanitize to the
     * identical string {@code "mp3"}. Combined with {@link #TIMESTAMP_FORMAT}'s one-second
     * resolution, any two uploads for the same language landing in the same second previously
     * produced the exact same blob path, and {@link #uploadFile}'s {@code overwrite=true} meant
     * the second upload silently replaced the first — both callers' stored URLs then served
     * whichever content was uploaded last. Reported directly from real use: a learner's "You
     * said" line and its "Corrected" line synthesize back-to-back within the same {@code
     * processTurn} call (see {@code ConversationOrchestrator#persistLearnerTurn}), landing in the
     * same second far more often than not — the "Corrected" play button was audibly playing the
     * "You said" audio. {@code uniqueSuffix} (8 hex characters from a random UUID) makes every
     * blob name unique regardless of the source text's script or how many uploads land in the
     * same timestamp second — a fix for every caller of this method, not just the one that
     * surfaced it.
     *
     * @param fileName Original filename
     * @param fileType Type of file (IMAGE, AUDIO, VIDEO)
     * @param language Language code (used for audio/video only)
     * @return Generated blob name
     */
    public String generateBlobName(String fileName, FileType fileType, String language) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String sanitizedFileName = sanitizeFileName(fileName);
        String typeFolder = fileType.getContainerSuffix();

        String blobName;
        if (fileType == FileType.IMAGE) {
            // Images are shared across all languages: images/{timestamp}_{uniqueSuffix}_{sanitizedFilename}
            blobName = String.format("%s/%s_%s_%s",
                typeFolder,
                timestamp,
                uniqueSuffix,
                sanitizedFileName
            );
        } else {
            // Audio and video are language-specific
            blobName = String.format("%s/%s/%s_%s_%s",
                typeFolder,
                language.toLowerCase().trim(),
                timestamp,
                uniqueSuffix,
                sanitizedFileName
            );
        }

        logger.info("[generateBlobName] fileName={}, fileType={}, language={} -> blobName={}",
            fileName, fileType, language, blobName);

        return blobName;
    }
    
    /**
     * Searches for media files by questionText (English word) in Azure Blob Storage.
     * Returns all matching blob names sorted by timestamp (newest first).
     * 
     * Search patterns:
     * - Images: images/*_{questionText}.{ext}
     * - Audio: audio/{language}/*_{questionText}.{ext}
     * - Video: video/{language}/*_{questionText}.{ext}
     * 
     * Example: findMediaByWord("Tree", IMAGE, "tamil") searches for:
     * - images/*_tree.png, images/*_tree.jpg, etc.
     * 
     * @param questionText English word from Asset.questionText (e.g., "Tree", "Dog")
     * @param fileType Type of media (IMAGE, AUDIO, VIDEO)
     * @param language Language code (used for audio/video only)
     * @return List of blob names matching the pattern, sorted newest first
     */
    public List<String> findMediaByWord(String questionText, FileType fileType, String language) {
        if (questionText == null || questionText.trim().isEmpty()) {
            logger.warn("questionText is null or empty, returning empty list");
            return new ArrayList<>();
        }
        
        String normalizedWord = questionText.toLowerCase().trim();
        String containerName = fileType.getContainerName();
        String prefix;
        
        // Determine search prefix based on file type
        if (fileType == FileType.IMAGE) {
            // Images are shared across all languages: images/{timestamp}_{word}.ext
            prefix = "images/";
        } else {
            prefix = fileType.getContainerSuffix() + "/" + language.toLowerCase().trim() + "/"; // Language-specific
        }
        
        logger.debug("Searching for media: questionText={}, fileType={}, language={}, prefix={}", 
            questionText, fileType, language, prefix);
        
        try {
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            
            if (!containerClient.exists()) {
                logger.warn("Container does not exist: {}", containerName);
                return new ArrayList<>();
            }
            
            // List all blobs with the prefix
            ListBlobsOptions options = new ListBlobsOptions().setPrefix(prefix);
            List<String> matchingBlobs = new ArrayList<>();
            
            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                String blobName = blobItem.getName();
                
                // Extract filename from blob path (last segment)
                String[] parts = blobName.split("/");
                String filename = parts[parts.length - 1];
                
                // Support two naming patterns:
                // 1. New format: {timestamp}_{word}.{ext}  e.g. 20260507120000_tree.png
                // 2. Legacy format: {word}.{ext}  e.g. tree.png
                String filenameLower = filename.toLowerCase();
                String filenameLowerNoExt = filenameLower.contains(".")
                        ? filenameLower.substring(0, filenameLower.lastIndexOf('.'))
                        : filenameLower;

                boolean matches = false;
                if (filenameLowerNoExt.equals(normalizedWord)) {
                    // Direct match: dog.png
                    matches = true;
                } else if (filename.contains("_")) {
                    // Timestamp prefix: 20260507120000_dog.png
                    String afterUnderscore = filename.substring(filename.indexOf('_') + 1).toLowerCase();
                    String afterUnderscoreNoExt = afterUnderscore.contains(".")
                            ? afterUnderscore.substring(0, afterUnderscore.lastIndexOf('.'))
                            : afterUnderscore;
                    matches = afterUnderscoreNoExt.equals(normalizedWord);
                }

                if (matches) {
                    matchingBlobs.add(blobName);
                    logger.debug("Found matching blob: {}", blobName);
                }
            }
            
            // Sort by blob name (which includes timestamp) - newest first
            matchingBlobs.sort(Comparator.reverseOrder());
            
            logger.info("Found {} matching blobs for questionText={}, fileType={}, language={}", 
                matchingBlobs.size(), questionText, fileType, language);
            
            return matchingBlobs;
            
        } catch (IllegalStateException e) {
            logger.warn("Azure media search unavailable for questionText={}, fileType={}, language={}: {}",
                questionText, fileType, language, e.getMessage());
            return new ArrayList<>();
        } catch (BlobStorageException e) {
            logger.error("Error searching for media: questionText={}, fileType={}, language={}: {}",
                questionText, fileType, language, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Unexpected error searching for media: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Lists every blob name under the given prefix in a single call — for callers
     * that need to check "does X have media" for many items at once (e.g. rendering
     * a grid of letters) without making one findMediaByWord round trip per item.
     */
    public List<String> listBlobNames(FileType fileType, String prefix) {
        if (!isConfigured()) return new ArrayList<>();
        String containerName = fileType.getContainerName();
        try {
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) return new ArrayList<>();

            List<String> names = new ArrayList<>();
            for (BlobItem blobItem : containerClient.listBlobs(new ListBlobsOptions().setPrefix(prefix), null)) {
                names.add(blobItem.getName());
            }
            return names;
        } catch (Exception e) {
            logger.warn("Failed to list blobs for container={}, prefix={}: {}", containerName, prefix, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Finds the latest (most recent) media file for a given questionText.
     * Returns the full blob URL if found, or null if no matching file exists.
     * 
     * @param questionText English word from Asset.questionText
     * @param fileType Type of media (IMAGE, AUDIO, VIDEO)
     * @param language Language code (used for audio/video only)
     * @return Full blob URL of the latest matching file, or null if not found
     */
    public String findLatestMediaFile(String questionText, FileType fileType, String language) {
        if (fileType == FileType.IMAGE) {
            return findLatestImageUrl(questionText, language);
        }

        List<String> matchingBlobs = findMediaByWord(questionText, fileType, language);

        if (matchingBlobs.isEmpty()) {
            logger.debug("No media found for questionText={}, fileType={}, language={}",
                questionText, fileType, language);
            return null;
        }

        // Get the first blob (most recent due to sorting)
        String blobName = matchingBlobs.get(0);
        String blobUrl = buildBlobUrl(fileType.getContainerName(), blobName);
        if (blobUrl != null) {
            logger.info("Latest media found: questionText={}, fileType={}, language={}, url={}",
                questionText, fileType, language, blobUrl);
        }
        return blobUrl;
    }

    /**
     * Resolves the preferred image URL for a word, checking both the legacy flat-named
     * container and the structured (timestamped) container, and preferring a .jpg/.jpeg
     * candidate over .png/.gif when both are available for the same word.
     */
    private String findLatestImageUrl(String questionText, String language) {
        String legacyUrl = findLegacyImageUrl(questionText.toLowerCase().trim());

        List<String> matchingBlobs = findMediaByWord(questionText, FileType.IMAGE, language);
        String structuredUrl = matchingBlobs.isEmpty()
                ? null
                : buildBlobUrl(FileType.IMAGE.getContainerName(), matchingBlobs.get(0));

        String preferredUrl = preferJpgThenStructured(legacyUrl, structuredUrl);
        if (preferredUrl != null) {
            logger.info("Resolved image for questionText={}, language={}, url={}", questionText, language, preferredUrl);
        } else {
            logger.debug("No image found for questionText={}, language={}", questionText, language);
        }
        return preferredUrl;
    }

    /**
     * Picks the preferred candidate between a legacy-container URL and a structured-container
     * URL for the same word: a .jpg/.jpeg candidate always wins over a non-jpg one; if both or
     * neither are jpg, the structured (timestamped, more recently uploaded) candidate wins.
     */
    static String preferJpgThenStructured(String legacyUrl, String structuredUrl) {
        boolean legacyIsJpg = isJpgUrl(legacyUrl);
        boolean structuredIsJpg = isJpgUrl(structuredUrl);

        if (legacyIsJpg && !structuredIsJpg) {
            return legacyUrl;
        }
        return structuredUrl != null ? structuredUrl : legacyUrl;
    }

    private static boolean isJpgUrl(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private String buildBlobUrl(String containerName, String blobName) {
        try {
            BlobServiceClient serviceClient = getBlobServiceClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            return blobClient.getBlobUrl();
        } catch (Exception e) {
            logger.error("Error getting blob URL for {}: {}", blobName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Sanitizes a filename by removing or replacing unsafe characters.
     * Keeps alphanumeric characters, dots, hyphens, and underscores.
     * Replaces spaces with underscores.
     * Removes leading/trailing spaces and dots.
     * 
     * @param fileName Original filename
     * @return Sanitized filename
     */
    /**
     * Checks the legacy image container for a blob named exactly {word}.{ext}.
     * This handles images uploaded before the timestamp-prefix naming convention.
     * Container: language-tool-images-container, blobs: dog.png, cat.jpg, etc.
     */
    private String findLegacyImageUrl(String normalizedWord) {
        String[] extensions = {"jpg", "jpeg", "png", "gif"};
        String legacyContainer = "language-tool-images-container";
        try {
            BlobContainerClient containerClient = getBlobServiceClient().getBlobContainerClient(legacyContainer);
            if (!containerClient.exists()) {
                return null;
            }
            for (String ext : extensions) {
                BlobClient blobClient = containerClient.getBlobClient(normalizedWord + "." + ext);
                if (blobClient.exists()) {
                    logger.debug("Found legacy image: {}.{}", normalizedWord, ext);
                    return blobClient.getBlobUrl();
                }
            }
        } catch (IllegalStateException e) {
            logger.warn("Legacy image lookup unavailable for {}: {}", normalizedWord, e.getMessage());
        } catch (Exception e) {
            logger.warn("Error checking legacy image container for {}: {}", normalizedWord, e.getMessage());
        }
        return null;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unnamed";
        }
        
        // Remove path separators and dangerous characters
        String sanitized = fileName.replaceAll("[/\\\\]", "");
        
        // Replace spaces with underscores
        sanitized = sanitized.replace(' ', '_');
        
        // Keep only safe characters: alphanumeric, dots, hyphens, underscores
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "");
        
        // Remove leading/trailing dots and underscores
        sanitized = sanitized.replaceAll("^[._]+|[._]+$", "");
        
        // Ensure filename is not empty after sanitization
        if (sanitized.isEmpty()) {
            sanitized = "unnamed";
        }
        
        return sanitized;
    }
}
