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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tms.backend.dto.TomatoSizingResponse;


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
        return sendFilesToTomatoAPIByPath(filePaths, null, null, null, null);
    }

    /**
     * Submits files to the Tomato sizing API and returns the Tomato jobId immediately.
     * Use fetchSizingResultOnce(jobId) to check progress.
     */
    public String sendFilesToTomatoAPIByPath(List<String> filePaths, String sizingRequestJson, Long tmId,
            String sourceLanguage, String targetLanguage) {
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
            if (sourceLanguage != null) {
                body.add("sourceLanguage", sourceLanguage);
            }
            if (targetLanguage != null) {
                body.add("targetLanguage", targetLanguage);
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
     * Polls GET /api/Sizing/sizing-progress/{jobId} once.
     * Returns a SizingPollStatus with progressPercent always set.
     * When progressPercent reaches 100, fetches the full result from GET /api/Sizing/sizing-result/{jobId}.
     * result() is non-null only when progress is complete.
     * Throws if status is "failed" or the job is not found (404).
     */
    public SizingPollStatus fetchSizingResultOnce(String tomatoJobId) {
        try {
            ResponseEntity<String> progressResponse = restTemplate.getForEntity(
                    baseUrl + "/api/Sizing/sizing-progress/" + tomatoJobId, String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode progressRoot = mapper.readTree(progressResponse.getBody());

            String status = progressRoot.path("status").asText();
            double progressPercent = progressRoot.path("progressPercent").asDouble(0.0);
            String currentStage = progressRoot.path("currentStage").asText(null);
            int totalFiles = progressRoot.path("totalFiles").asInt(0);
            int processedFiles = progressRoot.path("processedFiles").asInt(0);
            int totalSegments = progressRoot.path("totalSegments").asInt(0);
            int processedSegments = progressRoot.path("processedSegments").asInt(0);
            log.info("Sizing job {} status: {}, progress: {}%", tomatoJobId, status, progressPercent);

            if ("failed".equalsIgnoreCase(status)) {
                String apiError = progressRoot.path("error").asText(null);
                String message = "Tomato sizing job " + tomatoJobId + " failed";
                if (apiError != null && !apiError.isBlank()) {
                    message += ": " + apiError;
                }
                throw new RuntimeException(message);
            }

            if (progressPercent < 100.0) {
                return new SizingPollStatus(progressPercent, currentStage, totalFiles, processedFiles, totalSegments, processedSegments, null);
            }

            // Progress is 100%, fetch the full result
            ResponseEntity<String> resultResponse = restTemplate.getForEntity(
                    baseUrl + "/api/Sizing/sizing-result/" + tomatoJobId, String.class
            );

            JsonNode resultRoot = mapper.readTree(resultResponse.getBody());
            ObjectNode resultNode = (ObjectNode) resultRoot.path("result").deepCopy();
            resultNode.put("status", "completed");
            TomatoSizingResponse result = mapper.treeToValue(resultNode, TomatoSizingResponse.class);
            return new SizingPollStatus(progressPercent, currentStage, totalFiles, processedFiles, totalSegments, processedSegments, result);

        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Tomato sizing job not found: " + tomatoJobId, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch sizing result for jobId: " + tomatoJobId, e);
        }
    }

    /**
     * Submits a DITA file to the sizing API with returnXliff=true, then polls until the
     * sizing job completes. Returns the sizing stats and the tomatoJobId so the caller
     * can later retrieve the XLIFF via fetchXliffBytes(tomatoJobId).
     * Blocks until the sizing job finishes (up to 5 minutes).
     */
    public SizingWithXliffResult sendDitaFileAndGetXliff(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("ditaFiles", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalName;
                }
            });
            body.add("returnXliff", "true");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    baseUrl + "/api/Sizing/sizing-from-dita", requestEntity, String.class
            );

            String tomatoJobId = extractTomatoJobId(rawResponse.getBody());
            log.info("Sizing+XLIFF job submitted, tomatoJobId: {}", tomatoJobId);

            // Poll until complete
            TomatoSizingResponse sizingResponse = null;
            for (int attempt = 1; attempt <= 60 && sizingResponse == null; attempt++) {
                try {
                    Thread.sleep(5_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                SizingPollStatus pollStatus = fetchSizingResultOnce(tomatoJobId);
                sizingResponse = pollStatus.result();
                log.info("Waiting for sizing+XLIFF result (attempt {}/60)", attempt);
            }

            if (sizingResponse == null) {
                throw new RuntimeException("Sizing+XLIFF job " + tomatoJobId + " did not complete in time");
            }

            log.info("Sizing complete for tomatoJobId: {}. XLIFF available via fetchXliffBytes.", tomatoJobId);
            return new SizingWithXliffResult(sizingResponse, tomatoJobId);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to submit DITA file for sizing+XLIFF: {}", e.getMessage());
            throw new RuntimeException("Failed to submit DITA file for sizing+XLIFF", e);
        }
    }

    /**
     * Retrieves the XLIFF file generated by a completed sizing job.
     */
    public byte[] fetchXliffBytes(String tomatoJobId) {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                baseUrl + "/api/Sizing/sizing-xliff/" + tomatoJobId, byte[].class
        );
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to fetch XLIFF for tomatoJobId: " + tomatoJobId);
        }
        return response.getBody();
    }

    /**
     * Deletes a sizing job from the Tomato server after its results have been saved locally.
     */
    public void deleteSizingJob(String tomatoJobId) {
        try {
            restTemplate.delete(baseUrl + "/api/Sizing/sizing-jobs/" + tomatoJobId);
            log.info("Deleted sizing job {} from Tomato server", tomatoJobId);
        } catch (Exception e) {
            log.warn("Failed to delete sizing job {} from Tomato server: {}", tomatoJobId, e.getMessage());
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
