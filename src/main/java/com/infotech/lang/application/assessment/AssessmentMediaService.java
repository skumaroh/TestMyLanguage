package com.infotech.lang.application.assessment;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads word pictures and audio from Blob Storage with read-only, container-scoped tokens: one for the images container
 * and one for the audio container (each token works for its own container only).
 */
@Service
public class AssessmentMediaService {

    public static final String AUDIO_CONTAINER = "language-tool-audio";
    public static final String IMAGE_CONTAINER = "language-tool-images";

    private static final Logger logger = LoggerFactory.getLogger(AssessmentMediaService.class);

    private final String imageConnectionString;
    private final String audioConnectionString;
    private final Map<String, BlobServiceClient> clients = new ConcurrentHashMap<>();

    public AssessmentMediaService(@Value("${azure.storage.connection-string:}") String imageConnectionString,
                                  @Value("${azure.storage.audio-connection-string:}") String audioConnectionString) {
        this.imageConnectionString = imageConnectionString;
        this.audioConnectionString = audioConnectionString;
    }

    /** True for the two containers the proxy may serve for the given kind ({@code audio} or {@code image}). */
    public static boolean isAllowed(String kind, String container) {
        return ("audio".equals(kind) && AUDIO_CONTAINER.equals(container))
                || ("image".equals(kind) && IMAGE_CONTAINER.equals(container));
    }

    /** The blob's bytes, or empty when it does not exist or cannot be read. Never throws. */
    public Optional<byte[]> read(String container, String blobPath) {
        String connectionString = AUDIO_CONTAINER.equals(container) ? audioConnectionString : imageConnectionString;
        if (connectionString == null || connectionString.isBlank()) {
            logger.warn("No Blob access configured for container {}", container);
            return Optional.empty();
        }
        try {
            BlobServiceClient client = clients.computeIfAbsent(connectionString,
                    cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient());
            return Optional.of(client.getBlobContainerClient(container).getBlobClient(blobPath).downloadContent().toBytes());
        } catch (BlobStorageException e) {
            logger.warn("Blob read failed for {}/{}: status {}", container, blobPath, e.getStatusCode());
            return Optional.empty();
        } catch (RuntimeException e) {
            logger.warn("Blob read failed for {}/{}: {}", container, blobPath, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
