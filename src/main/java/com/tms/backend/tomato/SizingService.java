package com.tms.backend.tomato;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.backend.dto.TomatoSizingResponse;
import org.springframework.web.client.HttpClientErrorException;


@Service
public class SizingService {

    private static final Logger log = LoggerFactory.getLogger(SizingService.class);

    private final RestTemplate restTemplate;

    @Value("${tomato.api.url}")
    private String baseUrl;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public SizingService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public TomatoSizingResponse sendFilesToTomatoAPI(List<MultipartFile> files) {
        try {
            String firstFilename = files.get(0).getOriginalFilename();
            boolean isXliff = firstFilename != null && (firstFilename.endsWith(".xliff") || firstFilename.endsWith(".xlf"));
            String fileKey = isXliff ? "xliffFiles" : "ditaFiles";
            String endpoint = isXliff ? "/api/Sizing/sizing-from-xliff" : "/api/Sizing/sizing-from-dita";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            for (MultipartFile file : files) {
                String filename = file.getOriginalFilename();
                body.add(fileKey, new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                });
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    baseUrl + endpoint, requestEntity, String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(rawResponse.getBody(), TomatoSizingResponse.class);
        } catch (Exception e) {
            System.err.println("Failed to send file to tomato API: " + e.getMessage());
            throw new RuntimeException("Failed to send file to tomato API", e);
        }
    }

    public String sendFilesToTomatoAPIByPath(List<String> filePaths) {
        return sendFilesToTomatoAPIByPath(filePaths, null, null);
    }

    /**
     * Submits files to the Tomato sizing API and returns the Tomato jobId immediately.
     * Use fetchSizingResultOnce(jobId) to check progress.
     */
    public String sendFilesToTomatoAPIByPath(List<String> filePaths, String sizingRequestJson, Long tmId) {
        String firstFilename = Paths.get(filePaths.get(0)).getFileName().toString();
        boolean isXliff = firstFilename.endsWith(".xliff") || firstFilename.endsWith(".xlf");
        String fileKey = isXliff ? "xliffFiles" : "ditaFiles";
        String endpoint = isXliff ? "/api/Sizing/sizing-from-xliff" : "/api/Sizing/sizing-from-dita";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (String filePath : filePaths) {
            Path path = Paths.get(uploadDir).resolve(filePath);
            String filename = path.getFileName().toString();

            byte[] fileBytes;
            try {
                fileBytes = Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file from path: " + filePath, e);
            }

            body.add(fileKey, new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
        }

        try {
            if (tmId != null) {
                body.add("tmId", String.valueOf(tmId));
            }
            if (sizingRequestJson != null) {
                body.add("sizingRequestJson", sizingRequestJson);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    baseUrl + endpoint, requestEntity, String.class
            );

            String tomatoJobId = extractTomatoJobId(rawResponse.getBody());
            log.info("Tomato sizing job submitted, jobId: {}", tomatoJobId);
            return tomatoJobId;
        } catch (Exception e) {
            log.error("Failed to submit sizing job to Tomato API: {}", e.getMessage());
            throw new RuntimeException("Failed to submit sizing job to Tomato API", e);
        }
    }

    /**
     * Polls GET /api/Sizing/sizing-result/{jobId} once.
     * Returns the result if status is "completed", null if still queued/processing.
     * Throws if status is "failed" or the job is not found (404).
     */
    public TomatoSizingResponse fetchSizingResultOnce(String tomatoJobId) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/api/Sizing/sizing-result/" + tomatoJobId, String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode root = mapper.readTree(response.getBody());

            String status = root.path("status").asText();
            log.info("Sizing job {} status: {}", tomatoJobId, status);

            if ("completed".equalsIgnoreCase(status)) {
                TomatoSizingResponse result = mapper.treeToValue(root.path("result"), TomatoSizingResponse.class);
                return result;
            }
            if ("failed".equalsIgnoreCase(status)) {
                throw new RuntimeException("Tomato sizing job " + tomatoJobId + " failed");
            }

            log.info("Sizing job {} still processing, status: {}", tomatoJobId, status);
            return null;

        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Tomato sizing job not found: " + tomatoJobId, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch sizing result for jobId: " + tomatoJobId, e);
        }
    }

    private String extractTomatoJobId(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(responseBody);
            if (node.has("jobId")) return node.get("jobId").asText();
            if (node.has("id")) return node.get("id").asText();
        } catch (Exception ignored) {
        }
        return responseBody.trim().replace("\"", "");
    }
}
