package com.tms.backend.tomato;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.job.Job;
import com.tms.backend.job.Job.OriginalFileFormat;

@Service
public class FileConversionService {
    private static final Logger logger = LoggerFactory.getLogger(FileConversionService.class);

    private final RestTemplate restTemplate;

    @Value("${tomato.api.url}")
    private String baseUrl;

    @Value("${conversion.output.directory:converted-files}")
    private String outputDirectory;

    @Value("${file.upload-dir}")
    private String uploadDir;


    public FileConversionService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    /**
     * Upload a MultipartFile to the conversion API and save the converted file locally.
     * Uses the original filename for the output file.
     *
     * @param file The MultipartFile to convert (xml to xliff OR sdlxliff to xliff)
     * @return Path to the saved converted file
     * @throws IOException if file operations fail
     */
    public Path uploadAndConvertFile(MultipartFile file, String projectFolderName, String jobFolderName, Job job,
            Integer minSimilarity, Boolean preTranslate, Integer autoApplyScore, Long tmId) throws IOException {
        return uploadAndConvertFile(file.getBytes(), file.getOriginalFilename(), file.getContentType(), file.getSize(),
                projectFolderName, jobFolderName, job, minSimilarity, preTranslate, autoApplyScore, tmId);
    }

    /**
     * Same as {@link #uploadAndConvertFile(MultipartFile, String, String, Job, Integer, Boolean, Integer, Long)},
     * but takes the file content as an in-memory byte array instead of a MultipartFile. Use this when the same
     * uploaded file needs to be sent to the conversion API more than once (e.g. once per target language): a
     * servlet-backed MultipartFile can only be safely persisted via transferTo() a single time, since the
     * container may move/delete its underlying temp file on first use.
     *
     * @return Path to the saved converted file
     * @throws IOException if file operations fail
     */
    public Path uploadAndConvertFile(byte[] fileBytes, String originalFilename, String contentType, long fileSize,
            String projectFolderName, String jobFolderName, Job job,
            Integer minSimilarity, Boolean preTranslate, Integer autoApplyScore, Long tmId) throws IOException {
        try {
            logger.info("Uploading file {} to conversion API", originalFilename);

            // Determine file extension
            FileType fileType = detectFileType(fileBytes, originalFilename);

            // Select correct API endpoint
            String endpoint;
            switch (fileType) {
                case SDLXLIFF:
                    endpoint = baseUrl + "/api/DocumentConversion/sdlxliff-to-xliff";
                    break;

                case XML:
                    endpoint = baseUrl + "/api/DocumentConversion/dita-to-xliff";
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported file type: " + fileType);
            }

            // Build multipart request body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            });

            if (job.getSourceLang() != null) {
                body.add("sourceLanguage", job.getSourceLang());
            }
            if (job.getTargetLangs() != null && !job.getTargetLangs().isEmpty()) {
                body.add("targetLanguage", job.getTargetLangs().iterator().next());
            }
            if (minSimilarity != null) {
                body.add("minSimilarity", minSimilarity);
            }
            if (Boolean.TRUE.equals(preTranslate)) {
                body.add("autoApplyPerfectMatches", "true");
            }
            if (autoApplyScore != null) {
                body.add("autoApplyScore", autoApplyScore);
            }
            if (tmId != null) {
                body.add("tmId", tmId);
            }

            logger.info("Sending conversion request to {} with body: file={} ({} bytes), sourceLanguage={}, targetLanguage={}, minSimilarity={}, autoApplyPerfectMatches={}, autoApplyScore={}, tmId={}",
                    endpoint,
                    originalFilename,
                    fileSize,
                    body.getFirst("sourceLanguage"),
                    body.getFirst("targetLanguage"),
                    body.getFirst("minSimilarity"),
                    body.getFirst("autoApplyPerfectMatches"),
                    body.getFirst("autoApplyScore"),
                    body.getFirst("tmId"));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Make API request with retry
            ResponseEntity<byte[]> response = callWithRetry(endpoint, requestEntity, 3, 5000);

            logger.info("Successfully received converted file from API. Status: {}", response.getStatusCode());

            // Save the converted file using the original filename
            Path savedPath = saveConvertedFile(
                response.getBody(),
                originalFilename,
                projectFolderName,
                jobFolderName,
                fileBytes,
                fileSize,
                contentType,
                job,
                fileType
            );

            logger.info("Converted file saved to: {}", savedPath.toAbsolutePath());

            return savedPath;

        } catch (RestClientException e) {
            logger.error("Failed to send file to conversion API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send file to conversion API", e);
        }
    }

    /**
     * Save the converted file bytes to the local filesystem.
     *
     * @param fileBytes The file content as bytes
     * @param fileName The name for the saved file
     * @return Path to the saved file
     * @throws IOException if file operations fail
     */
    private Path saveConvertedFile(
        byte[] fileBytes,
        String fileName,
        String projectFolderName,
        String jobFolderName,
        byte[] originalBytes,
        long originalFileSize,
        String originalContentType,
        Job job,
        FileType detectedFileType
        ) throws IOException {
        // Get user's downloads folder
        Path baseDir = Paths.get(uploadDir);

        // Create full output directory: projects/{projectId}-{name}/jobs/file{N}/{jobId}-{langPair}
        Path outputDir = baseDir
                    .resolve("projects")
                    .resolve(projectFolderName)
                    .resolve("jobs")
                    .resolve(jobFolderName);

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            logger.info("Created output directory: {}", outputDir.toAbsolutePath());
        }

        // Create subdirectories for original and converted files
        Path originalDir = outputDir.resolve("original");
        Path convertedDir = outputDir.resolve("converted");

        Files.createDirectories(originalDir);
        Files.createDirectories(convertedDir);

        // Save the original uploaded file
        Path originalFilePath = originalDir.resolve(fileName);
        Files.write(originalFilePath, originalBytes);
        logger.info("Saved original file: {}", originalFilePath.toAbsolutePath());

        // Replace the file extension with .xliff
        String xliffFileName = fileName.replaceFirst("\\.[^.]+$", ".xliff");

        // Create full output path
        Path outputPath = convertedDir.resolve(xliffFileName);

        // Write the file
        Files.write(outputPath, fileBytes);

        logger.info("Saved converted file: {}", outputPath.toAbsolutePath());

        // Calculate relative paths
        Path relativeOriginalPath = baseDir.relativize(originalFilePath);
        Path relativeConvertedPath = baseDir.relativize(outputPath);

        // Update job with file information
        job.setOriginalFileName(fileName);
        job.setOriginalFilePath(relativeOriginalPath.toString().replace("\\", "/"));
        job.setConvertedFileName(xliffFileName);
        job.setConvertedFilePath(relativeConvertedPath.toString().replace("\\", "/"));
        job.setFileUploadDate(LocalDateTime.now());
        job.setFileSize(originalFileSize);
        job.setContentType(originalContentType);

        switch (detectedFileType) {
            case SDLXLIFF -> job.setOriginalFileFormat(OriginalFileFormat.SDLXLIFF);
            case XML -> job.setOriginalFileFormat(OriginalFileFormat.XML);
            default -> job.setOriginalFileFormat(OriginalFileFormat.UNKNOWN);
        }

        logger.info("Updated job. Original format: {}, Original file path: {}, Converted file path: {}",
            job.getOriginalFileFormat(),
            job.getOriginalFilePath(),
            job.getConvertedFilePath()
        );

        return outputPath;
    }

    /**
     * Saves only the original DITA file to disk and updates the job's original-file fields.
     * Call this during sizing-during-creation so the file is persisted while the XLIFF
     * is deferred until the user explicitly requests it.
     */
    public Path saveOriginalFileOnly(
            MultipartFile originalFile,
            String projectFolderName,
            String jobFolderName,
            Job job) throws IOException {

        String originalName = originalFile.getOriginalFilename();
        Path baseDir = Paths.get(uploadDir);

        Path outputDir = baseDir
                .resolve("projects")
                .resolve(projectFolderName)
                .resolve("jobs")
                .resolve(jobFolderName);

        Path originalDir = outputDir.resolve("original");
        Files.createDirectories(originalDir);

        Path originalFilePath = originalDir.resolve(originalName);
        originalFile.transferTo(originalFilePath.toFile());
        logger.info("Saved original file: {}", originalFilePath.toAbsolutePath());

        Path relativeOriginalPath = baseDir.relativize(originalFilePath);

        job.setOriginalFileName(originalName);
        job.setOriginalFilePath(relativeOriginalPath.toString().replace("\\", "/"));
        job.setFileUploadDate(LocalDateTime.now());
        job.setFileSize(originalFile.getSize());
        job.setContentType(originalFile.getContentType());
        job.setOriginalFileFormat(OriginalFileFormat.XML);

        logger.info("Updated job with original file fields. Path: {}", job.getOriginalFilePath());
        return originalFilePath;
    }

    /**
     * Saves the XLIFF bytes (produced by the sizing API) to the job's converted directory
     * and updates the job's converted-file fields.
     * Call this when the user explicitly triggers XLIFF retrieval after sizing.
     *
     * @param jobDirectory the job's storage folder, relative to the upload dir (see JobService#resolveJobDirectory)
     */
    public Path saveXliffToConvertedDir(
            byte[] xliffBytes,
            Path jobDirectory,
            Job job) throws IOException {

        String originalName = job.getOriginalFileName();
        if (originalName == null) {
            throw new IllegalStateException("Job original file name is not set for job " + job.getId());
        }

        Path baseDir = Paths.get(uploadDir);

        Path convertedDir = baseDir.resolve(jobDirectory).resolve("converted");

        Files.createDirectories(convertedDir);

        String xliffFileName = originalName.replaceFirst("\\.[^.]+$", ".xliff");
        Path outputPath = convertedDir.resolve(xliffFileName);
        Files.write(outputPath, xliffBytes);
        logger.info("Saved XLIFF from sizing result: {}", outputPath.toAbsolutePath());

        Path relativeConvertedPath = baseDir.relativize(outputPath);

        job.setConvertedFileName(xliffFileName);
        job.setConvertedFilePath(relativeConvertedPath.toString().replace("\\", "/"));

        logger.info("Updated job with converted file fields. Path: {}", job.getConvertedFilePath());
        return outputPath;
    }

    /**
     * Copies the original (and, if present, converted) file from an already-created job into a
     * newly-created sibling job's own folder, without re-calling the conversion API. Used when a
     * job is fanned out into one Job per target language: every sibling job needs its own copy of
     * the same source content, but only the first job's upload actually goes through conversion.
     *
     * Note: if the source job's conversion was deferred (DITA sizing-during-creation, not yet
     * resolved via saveXliffFromSizing), only the original file is copied here; siblings created
     * before the source job's XLIFF is fetched won't automatically pick it up afterward.
     */
    public void copySourceFilesToSiblingJob(
            Job sourceJob,
            Job siblingJob,
            String projectFolderName,
            String siblingJobFolderName) throws IOException {

        Path baseDir = Paths.get(uploadDir);

        Path siblingOutputDir = baseDir
                .resolve("projects")
                .resolve(projectFolderName)
                .resolve("jobs")
                .resolve(siblingJobFolderName);

        if (sourceJob.getOriginalFilePath() != null) {
            Path sourceOriginalPath = baseDir.resolve(sourceJob.getOriginalFilePath());
            if (Files.exists(sourceOriginalPath)) {
                Path siblingOriginalDir = siblingOutputDir.resolve("original");
                Files.createDirectories(siblingOriginalDir);
                Path siblingOriginalPath = siblingOriginalDir.resolve(sourceJob.getOriginalFileName());
                Files.copy(sourceOriginalPath, siblingOriginalPath, StandardCopyOption.REPLACE_EXISTING);

                siblingJob.setOriginalFileName(sourceJob.getOriginalFileName());
                siblingJob.setOriginalFilePath(baseDir.relativize(siblingOriginalPath).toString().replace("\\", "/"));
            }
        }

        if (sourceJob.getConvertedFilePath() != null) {
            Path sourceConvertedPath = baseDir.resolve(sourceJob.getConvertedFilePath());
            if (Files.exists(sourceConvertedPath)) {
                Path siblingConvertedDir = siblingOutputDir.resolve("converted");
                Files.createDirectories(siblingConvertedDir);
                Path siblingConvertedPath = siblingConvertedDir.resolve(sourceJob.getConvertedFileName());
                Files.copy(sourceConvertedPath, siblingConvertedPath, StandardCopyOption.REPLACE_EXISTING);

                if (siblingJob.getTargetLangs() != null && !siblingJob.getTargetLangs().isEmpty()) {
                    updateXliffTargetLanguage(siblingConvertedPath, siblingJob.getTargetLangs().iterator().next());
                }

                siblingJob.setConvertedFileName(sourceJob.getConvertedFileName());
                siblingJob.setConvertedFilePath(baseDir.relativize(siblingConvertedPath).toString().replace("\\", "/"));
            }
        }

        siblingJob.setFileUploadDate(sourceJob.getFileUploadDate());
        siblingJob.setFileSize(sourceJob.getFileSize());
        siblingJob.setContentType(sourceJob.getContentType());
        siblingJob.setOriginalFileFormat(sourceJob.getOriginalFileFormat());

        logger.info("Copied source files from job {} to sibling job {}", sourceJob.getId(), siblingJob.getId());
    }

    // Sibling jobs are created by copying the representative job's already-converted XLIFF file
    // (see copySourceFilesToSiblingJob), so the <file target-language="..."> attribute in the
    // original conversion still reflects the representative's language. This rewrites it to the sibling's own
    // target language so the file content matches the Job record's target language.
    private static final Pattern TARGET_LANGUAGE_ATTR = Pattern.compile("target-language=\"[^\"]*\"");

    private void updateXliffTargetLanguage(Path xliffPath, String newTargetLang) throws IOException {
        String content = Files.readString(xliffPath, StandardCharsets.UTF_8);
        Matcher matcher = TARGET_LANGUAGE_ATTR.matcher(content);
        String updated = matcher.replaceFirst(Matcher.quoteReplacement("target-language=\"" + newTargetLang + "\""));
        if (!updated.equals(content)) {
            Files.writeString(xliffPath, updated, StandardCharsets.UTF_8);
        }
    }

    // In-memory result of a reverse conversion: the file name it should be downloaded as,
    // and its bytes. Never written to disk - the caller streams it straight to the client.
    public record GeneratedFile(String fileName, byte[] data) {
    }

    public GeneratedFile convertXliffBackToOriginalFormat(Job job) throws IOException {

        if (job.getOriginalFileFormat() == null) {
            throw new IllegalStateException("Original file format is not set for job " + job.getId());
        }

        // Locate the translated xliff file
        Path baseDir = Paths.get(uploadDir);
        Path xliffPath = baseDir.resolve(job.getTranslatedFilePath());

        if (!Files.exists(xliffPath)) {
            throw new FileNotFoundException("XLIFF file not found: " + xliffPath);
        }

        if (Files.size(xliffPath) == 0) {
            logger.warn("Translated file is empty. Using converted file instead. File: {}", job.getFileName());
            xliffPath = baseDir.resolve(job.getConvertedFilePath());

            if (!Files.exists(xliffPath)) {
                throw new FileNotFoundException("Converted XLIFF file not found: " + xliffPath);
            }
        }

        // Choose API endpoint & output extension
        String endpoint;
        String targetExtension;

        switch (job.getOriginalFileFormat()) {
            case SDLXLIFF -> {
                endpoint = baseUrl + "/api/DocumentConversion/xliff-to-sdlxliff";
                targetExtension = ".sdlxliff";
            }
            case XML -> {
                endpoint = baseUrl + "/api/DocumentConversion/export-dita";
                targetExtension = ".xml";
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported original file format: " + job.getOriginalFileFormat());
        }

        // Build multipart request
        Path resolvedXliffPath = xliffPath;
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(Files.readAllBytes(resolvedXliffPath)) {
            @Override
            public String getFilename() {
                return resolvedXliffPath.getFileName().toString();
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Call conversion API
        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                endpoint,
                requestEntity,
                byte[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Reverse conversion failed. Status: " + response.getStatusCode());
        }

        String targetFileName = job.getOriginalFileName().replaceFirst("\\.[^.]+$", targetExtension);

        logger.info(
            "Reverse conversion completed. Original format: {}, Target file: {}",
            job.getOriginalFileFormat(),
            targetFileName);

        return new GeneratedFile(targetFileName, response.getBody());
    }


    private ResponseEntity<byte[]> callWithRetry(
            String endpoint,
            HttpEntity<MultiValueMap<String, Object>> requestEntity,
            int maxRetries,
            long delayMs) {

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<byte[]> response = restTemplate.postForEntity(
                        endpoint, requestEntity, byte[].class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response;
                }

                logger.warn("Conversion API returned status {} on attempt {}/{}",
                        response.getStatusCode(), attempt, maxRetries);

            } catch (RestClientException e) {
                logger.warn("Conversion API call failed on attempt {}/{}: {}",
                        attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    throw new RuntimeException(
                            "Conversion API failed after " + maxRetries + " attempts", e);
                }
            }

            try {
                logger.info("Retrying in {} seconds...", delayMs / 1000);
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry interrupted", ie);
            }
        }

        throw new RuntimeException("Conversion API failed after " + maxRetries + " attempts");
    }

    private enum FileType {
        XML,
        SDLXLIFF,
        UNKNOWN
    }

    private FileType detectFileType(byte[] fileBytes, String filename) {

        String lowerName = filename != null ? filename.toLowerCase() : "";

        String content = new String(fileBytes, StandardCharsets.UTF_8);

        boolean hasSdlNamespace = content.contains("http://sdl.com/FileTypes/SdlXliff");

        boolean looksXml = content.trim().startsWith("<");

        // SDLXLIFF detection
        if (lowerName.endsWith(".sdlxliff") || hasSdlNamespace) {
            return FileType.SDLXLIFF;
        }

        // XML detection
        if (lowerName.endsWith(".xml") || looksXml) {
            return FileType.XML;
        }

        return FileType.UNKNOWN;
    }

}
