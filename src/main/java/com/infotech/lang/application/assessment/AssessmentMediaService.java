package com.infotech.lang.application.assessment;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    public static final String REFERENCE_CONTAINER = "language-tool-letter-references";
    /** The video clips and action pictures of the assessment live in the image container, under this folder (owner decision). */
    public static final String VIDEO_CONTAINER = IMAGE_CONTAINER;
    public static final String VIDEO_PREFIX = "assessment-clips/";

    private static final Logger logger = LoggerFactory.getLogger(AssessmentMediaService.class);

    private final String imageConnectionString;
    private final String audioConnectionString;
    private final String referenceConnectionString;
    /** A separate connection string that may also write to the images container (empty = the app can only read pictures). */
    private final String imageWriteConnectionString;
    private final Map<String, BlobServiceClient> clients = new ConcurrentHashMap<>();

    /** Letter recordings are named {@code [timestamp_]u0B95[-u0BCD].ext}: the key is the letter's code points. */
    private static final Pattern LETTER_FILE = Pattern.compile("^(?:\\d+_)?(u[0-9a-f]{4,6}(?:-u[0-9a-f]{4,6})*)$");
    private static final long LETTER_INDEX_MINUTES = 10;

    private record LetterIndex(Map<String, String> blobsByKey, Instant loadedAt) {
    }

    private final Map<String, LetterIndex> letterIndexes = new ConcurrentHashMap<>();
    /** When listing a language's letter recordings last failed; a failure is not retried for a minute. */
    private final Map<String, Instant> letterIndexFailures = new ConcurrentHashMap<>();
    private static final long LETTER_RETRY_SECONDS = 60;

    /** Word pictures are found by name: {@code images/[timestamp_[hash_]]word.ext}; the index is rebuilt every ten minutes. */
    private static final Pattern IMAGE_FILE = Pattern.compile("^(?:\\d+_(?:[0-9a-f]{8}_)?)?(.+)$");
    private volatile LetterIndex imageIndex;
    private volatile Instant imageIndexFailedAt;

    public AssessmentMediaService(@Value("${azure.storage.connection-string:}") String imageConnectionString,
                                  @Value("${azure.storage.audio-connection-string:}") String audioConnectionString,
                                  @Value("${azure.storage.reference-connection-string:}") String referenceConnectionString,
                                  @Value("${azure.storage.image-write-connection-string:}") String imageWriteConnectionString) {
        this.imageWriteConnectionString = imageWriteConnectionString;
        this.imageConnectionString = imageConnectionString;
        this.audioConnectionString = audioConnectionString;
        this.referenceConnectionString = referenceConnectionString;
    }

    /** True for the two containers the proxy may serve for the given kind ({@code audio} or {@code image}). */
    public static boolean isAllowed(String kind, String container) {
        return ("audio".equals(kind) && AUDIO_CONTAINER.equals(container))
                || ("image".equals(kind) && (IMAGE_CONTAINER.equals(container) || REFERENCE_CONTAINER.equals(container)))
                || ("video".equals(kind) && VIDEO_CONTAINER.equals(container));
    }

    /** A blob name with its size in bytes. */
    public record BlobFile(String name, long sizeBytes) {
    }

    /**
     * The files under {@link #VIDEO_PREFIX} in the clips container (name and size), or empty when the container cannot be read.
     * Never throws.
     */
    public List<BlobFile> listClipFiles() {
        if (imageConnectionString == null || imageConnectionString.isBlank()) {
            return List.of();
        }
        try {
            BlobServiceClient client = clients.computeIfAbsent(imageConnectionString,
                    cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient());
            List<BlobFile> files = new ArrayList<>();
            for (BlobItem item : client.getBlobContainerClient(VIDEO_CONTAINER)
                    .listBlobs(new ListBlobsOptions().setPrefix(VIDEO_PREFIX), null)) {
                long size = item.getProperties() == null || item.getProperties().getContentLength() == null ? 0
                        : item.getProperties().getContentLength();
                files.add(new BlobFile(item.getName(), size));
            }
            return files;
        } catch (BlobStorageException e) {
            logger.warn("Could not list the clips: status {}, error code {}", e.getStatusCode(), e.getErrorCode());
            return List.of();
        } catch (RuntimeException e) {
            logger.warn("Could not list the clips: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    /** The blob's bytes, or empty when it does not exist or cannot be read. Never throws. */
    public Optional<byte[]> read(String container, String blobPath) {
        String connectionString = AUDIO_CONTAINER.equals(container) ? audioConnectionString
                : REFERENCE_CONTAINER.equals(container) ? referenceConnectionString : imageConnectionString;
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

    /** The audio key of a letter: its code points as {@code u0B95}, joined with {@code -} (lower case). */
    public static String audioKey(String letter) {
        StringBuilder key = new StringBuilder();
        letter.codePoints().forEach(cp -> {
            if (key.length() > 0) {
                key.append('-');
            }
            key.append(String.format(Locale.ROOT, "u%04x", cp));
        });
        return key.toString();
    }

    /** Maps each letter key to its newest recording among the given blob names (pure; used by the lookup below). */
    static Map<String, String> indexLetterAudio(List<String> blobNames) {
        Map<String, String> newest = new HashMap<>();
        for (String blobName : blobNames) {
            String filename = blobName.substring(blobName.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            int dot = filename.lastIndexOf('.');
            String stem = dot > 0 ? filename.substring(0, dot) : filename;
            Matcher m = LETTER_FILE.matcher(stem);
            if (m.matches()) {
                newest.merge(m.group(1), blobName, (a, b) -> compareAge(a, b) >= 0 ? a : b);
            }
        }
        return newest;
    }

    /** The recording of a letter (for example {@code audio/tamil/20260507_u0B95.mp3}), if one exists. Never throws. */
    public Optional<String> letterAudioBlob(String language, String letter) {
        if (language == null || language.isBlank() || letter == null || letter.isBlank()) {
            return Optional.empty();
        }
        String lang = language.toLowerCase(Locale.ROOT).trim();
        LetterIndex index = letterIndexes.get(lang);
        if (index == null || index.loadedAt().plusSeconds(LETTER_INDEX_MINUTES * 60).isBefore(Instant.now())) {
            Instant failedAt = letterIndexFailures.get(lang);
            if (failedAt != null && failedAt.plusSeconds(LETTER_RETRY_SECONDS).isAfter(Instant.now())) {
                return Optional.empty();
            }
            index = loadLetterIndex(lang);
            if (index == null) {
                letterIndexFailures.put(lang, Instant.now());
                return Optional.empty();
            }
            letterIndexFailures.remove(lang);
            letterIndexes.put(lang, index);
        }
        return Optional.ofNullable(index.blobsByKey().get(audioKey(letter)));
    }

    private LetterIndex loadLetterIndex(String lang) {
        if (audioConnectionString == null || audioConnectionString.isBlank()) {
            return null;
        }
        try {
            BlobServiceClient client = clients.computeIfAbsent(audioConnectionString,
                    cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient());
            List<String> names = new ArrayList<>();
            for (BlobItem item : client.getBlobContainerClient(AUDIO_CONTAINER)
                    .listBlobs(new ListBlobsOptions().setPrefix("audio/" + lang + "/"), null)) {
                names.add(item.getName());
            }
            return new LetterIndex(indexLetterAudio(names), Instant.now());
        } catch (BlobStorageException e) {
            logger.warn("Could not list letter audio for {}: status {}, error code {} (listing needs the List permission on the token)",
                    lang, e.getStatusCode(), e.getErrorCode());
            return null;
        } catch (RuntimeException e) {
            logger.warn("Could not list letter audio for {}: {}", lang, e.getClass().getSimpleName());
            return null;
        }
    }

    /** Orders two picture blob names by the number at the start of the file name (a bare name counts as oldest), then by name. */
    static int compareAge(String a, String b) {
        int byNumber = Long.compare(leadingNumber(a), leadingNumber(b));
        return byNumber != 0 ? byNumber : a.compareTo(b);
    }

    private static long leadingNumber(String blobName) {
        String file = blobName.substring(blobName.lastIndexOf('/') + 1);
        int end = 0;
        while (end < file.length() && end < 19 && Character.isDigit(file.charAt(end))) {
            end++;
        }
        try {
            return end == 0 ? 0 : Long.parseLong(file.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Every picture blob of each word (lower case), newest first (pure). Files with a bare name are the oldest. */
    static Map<String, List<String>> picturesByWord(List<String> blobNames) {
        Map<String, List<String>> byWord = new HashMap<>();
        for (String blobName : blobNames) {
            String filename = blobName.substring(blobName.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            int dot = filename.lastIndexOf('.');
            if (dot <= 0) {
                continue;
            }
            String ext = filename.substring(dot + 1);
            if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("webp"))) {
                continue;
            }
            Matcher m = IMAGE_FILE.matcher(filename.substring(0, dot));
            if (m.matches()) {
                byWord.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(blobName);
            }
        }
        byWord.values().forEach(list -> list.sort((x, y) -> compareAge(y, x)));
        return byWord;
    }

    /** A new picture's blob name: {@code images/<19 digit time>_<word>.<ext>}; the long number makes it newer than any older name. */
    static String newPictureName(String word, String ext, java.time.LocalDateTime now) {
        String stamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + "00";
        String key = word.toLowerCase(Locale.ROOT).trim().replaceAll("[^\\p{L}\\p{N}]+", "_").replaceAll("^_+|_+$", "");
        return "images/" + stamp + "_" + key + "." + ext;
    }

    /** True for a picture put here by the admin page (its name starts with the 19 digit time). */
    public static boolean madeByAdminPage(String blobName) {
        return leadingNumber(blobName) >= 1_000_000_000_000_000_000L;
    }

    /** Every picture blob under {@code images/}, grouped by word, newest first. Empty when the container cannot be listed. */
    public Map<String, List<String>> listPictures() {
        if (imageConnectionString == null || imageConnectionString.isBlank()) {
            return Map.of();
        }
        try {
            BlobServiceClient client = clients.computeIfAbsent(imageConnectionString,
                    cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient());
            List<String> names = new ArrayList<>();
            for (BlobItem item : client.getBlobContainerClient(IMAGE_CONTAINER)
                    .listBlobs(new ListBlobsOptions().setPrefix("images/"), null)) {
                names.add(item.getName());
            }
            return picturesByWord(names);
        } catch (RuntimeException e) {
            logger.warn("Could not list word pictures: {}", e.getClass().getSimpleName());
            return Map.of();
        }
    }

    public boolean canWritePictures() {
        return imageWriteConnectionString != null && !imageWriteConnectionString.isBlank();
    }

    /** Saves a new picture of the word as the newest one (older pictures stay as backups) and returns its blob name. */
    public String savePicture(String word, byte[] bytes, String ext) {
        if (!canWritePictures()) {
            throw new IllegalStateException("The app has no permission to save pictures");
        }
        String blobName = newPictureName(word, ext, java.time.LocalDateTime.now());
        BlobServiceClient client = clients.computeIfAbsent(imageWriteConnectionString,
                cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient());
        client.getBlobContainerClient(IMAGE_CONTAINER).getBlobClient(blobName)
                .upload(new java.io.ByteArrayInputStream(bytes), bytes.length, false);
        imageIndex = null;
        return blobName;
    }

    /** Removes a picture that the admin page made (never an older one), so the previous picture is current again. */
    public void removeAdminPicture(String blobName) {
        if (!canWritePictures() || !madeByAdminPage(blobName) || !blobName.startsWith("images/")) {
            throw new IllegalStateException("Only a picture made on the admin page can be removed");
        }
        BlobServiceClient client = clients.computeIfAbsent(imageWriteConnectionString,
                cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient());
        client.getBlobContainerClient(IMAGE_CONTAINER).getBlobClient(blobName).delete();
        imageIndex = null;
    }

    /** Maps each word (lower case) to its newest picture among the given blob names under {@code images/} (pure). */
    static Map<String, String> indexImages(List<String> blobNames) {
        Map<String, String> newest = new HashMap<>();
        for (String blobName : blobNames) {
            String filename = blobName.substring(blobName.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            int dot = filename.lastIndexOf('.');
            if (dot <= 0) {
                continue;
            }
            String ext = filename.substring(dot + 1);
            if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("webp"))) {
                continue;
            }
            String stem = filename.substring(0, dot);
            Matcher m = IMAGE_FILE.matcher(stem);
            if (m.matches()) {
                newest.merge(m.group(1), blobName, (a, b) -> compareAge(a, b) >= 0 ? a : b);
            }
            // the older naming without a hash, {timestamp}_{word}, is covered above; a bare {word}.ext is the stem itself
            newest.merge(stem, blobName, (a, b) -> compareAge(a, b) >= 0 ? a : b);
        }
        return newest;
    }

    /** The picture of a word (its English text, for example "Tree"), when one exists in the images container. Never throws. */
    public Optional<String> imageBlobForWord(String word) {
        if (word == null || word.isBlank()) {
            return Optional.empty();
        }
        LetterIndex index = imageIndex;
        if (index == null || index.loadedAt().plusSeconds(LETTER_INDEX_MINUTES * 60).isBefore(Instant.now())) {
            Instant failedAt = imageIndexFailedAt;
            if (failedAt != null && failedAt.plusSeconds(LETTER_RETRY_SECONDS).isAfter(Instant.now())) {
                return Optional.empty();
            }
            index = loadImageIndex();
            if (index == null) {
                imageIndexFailedAt = Instant.now();
                return Optional.empty();
            }
            imageIndexFailedAt = null;
            imageIndex = index;
        }
        String key = word.toLowerCase(Locale.ROOT).trim();
        String blob = index.blobsByKey().get(key);
        if (blob == null) {
            blob = index.blobsByKey().get(key.replace(' ', '_'));
        }
        return Optional.ofNullable(blob);
    }

    private LetterIndex loadImageIndex() {
        if (imageConnectionString == null || imageConnectionString.isBlank()) {
            return null;
        }
        try {
            BlobServiceClient client = clients.computeIfAbsent(imageConnectionString,
                    cs -> new BlobServiceClientBuilder().connectionString(cs).buildClient());
            List<String> names = new ArrayList<>();
            for (BlobItem item : client.getBlobContainerClient(IMAGE_CONTAINER)
                    .listBlobs(new ListBlobsOptions().setPrefix("images/"), null)) {
                names.add(item.getName());
            }
            return new LetterIndex(indexImages(names), Instant.now());
        } catch (BlobStorageException e) {
            logger.warn("Could not list word pictures: status {}, error code {}", e.getStatusCode(), e.getErrorCode());
            return null;
        } catch (RuntimeException e) {
            logger.warn("Could not list word pictures: {}", e.getClass().getSimpleName());
            return null;
        }
    }
}
