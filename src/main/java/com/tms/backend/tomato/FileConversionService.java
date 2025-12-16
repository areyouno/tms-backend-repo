package com.tms.backend.tomato;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

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
    public Path uploadAndConvertFile(MultipartFile file, String projectFolderName, String jobFolderName, Job job) throws IOException {
        try {
            String originalName = file.getOriginalFilename();
            logger.info("Uploading file {} to conversion API", originalName);
            
            // Determine file extension
            FileType fileType = detectFileType(file);

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
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalName;
                }
            });
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Make API request - expecting binary response
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                endpoint,
                requestEntity,
                byte[].class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("API returned unsuccessful status: " + response.getStatusCode());
            }
            
            logger.info("Successfully received converted file from API. Status: {}", response.getStatusCode());
            
            // Save the converted file using the original filename
            Path savedPath = saveConvertedFile(
                response.getBody(),
                originalName,
                projectFolderName,
                jobFolderName, 
                file,
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
        MultipartFile uploadedFile,
        Job job,
        FileType detectedFileType
        ) throws IOException {
        // Get user's downloads folder
        Path baseDir = Paths.get(uploadDir); 
        
        // Create full output directory: downloads/tomato/projects/projectId/jobs/jobsId
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
        uploadedFile.transferTo(originalFilePath.toFile());
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
        job.setFileUploadedAt(LocalDateTime.now());
        job.setFileSize(uploadedFile.getSize());
        job.setContentType(uploadedFile.getContentType());

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

    public Path convertXliffBackToOriginalFormat(
        Job job,
        String projectFolderName,
        String jobFolderName
        ) throws IOException {

        if (job.getOriginalFileFormat() == null) {
            throw new IllegalStateException("Original file format is not set for job " + job.getId());
        }

        // ---- Locate the converted xliff file ----
        Path baseDir = Paths.get(uploadDir);
        Path xliffPath = baseDir.resolve(job.getConvertedFilePath());

        if (!Files.exists(xliffPath)) {
            throw new FileNotFoundException("XLIFF file not found: " + xliffPath);
        }

        // ---- Choose API endpoint & output extension ----
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

        // ---- Build multipart request ----
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(Files.readAllBytes(xliffPath)) {
            @Override
            public String getFilename() {
                return xliffPath.getFileName().toString();
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // ---- Call conversion API ----
        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                endpoint,
                requestEntity,
                byte[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Reverse conversion failed. Status: " + response.getStatusCode());
        }

        // ---- Save target file ----
        Path targetDir = baseDir
                .resolve("projects")
                .resolve(projectFolderName)
                .resolve("jobs")
                .resolve(jobFolderName)
                .resolve("target");

        Files.createDirectories(targetDir);

        String targetFileName = job.getOriginalFileName().replaceFirst("\\.[^.]+$", targetExtension);

        Path targetFilePath = targetDir.resolve(targetFileName);
        Files.write(targetFilePath, response.getBody());

        // ---- Update job ----
        Path relativeTargetPath = baseDir.relativize(targetFilePath);
        job.setTranslatedFilePath(relativeTargetPath.toString().replace("\\", "/"));

        logger.info(
            "Reverse conversion completed. Original format: {}, Target path: {}",
            job.getOriginalFileFormat(),
            job.getTranslatedFilePath());

        return targetFilePath;
    }


    private enum FileType {
        XML,
        SDLXLIFF,
        UNKNOWN
    }

    private FileType detectFileType(MultipartFile file) throws IOException {

        String name = file.getOriginalFilename();
        String lowerName = name != null ? name.toLowerCase() : "";

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

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