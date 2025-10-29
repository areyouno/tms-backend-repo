package com.tms.backend.tomato;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
     * @param file The MultipartFile to convert
     * @return Path to the saved converted file
     * @throws IOException if file operations fail
     */ 
    public Path uploadAndConvertFile(MultipartFile file, String projectFolderName, String jobFolderName) throws IOException {
        try {
            logger.info("Uploading file {} to conversion API", file.getOriginalFilename());
            
            // Build multipart request body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Make API request - expecting binary response
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                baseUrl + "/api/DocumentConversion/dita-to-xliff",
                requestEntity,
                byte[].class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("API returned unsuccessful status: " + response.getStatusCode());
            }
            
            logger.info("Successfully received converted file from API. Status: {}", response.getStatusCode());
            
            // Save the converted file using the original filename
            Path savedPath = saveConvertedFile(response.getBody(), file.getOriginalFilename(), projectFolderName, jobFolderName, file);
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
    private Path saveConvertedFile(byte[] fileBytes, String fileName, String projectFolderName, String jobFolderName, MultipartFile uploadedFile) throws IOException {
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
        
        return outputPath;
    }
}