package com.tms.backend.tomato;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.tms.backend.dto.TomatoSizingResponse;
import com.tms.backend.netRateScheme.MatchType;


@Service
public class SizingService {
    
    private final RestTemplate restTemplate;

    @Value("${tomato.api.url}")
    private String baseUrl;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public SizingService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public TomatoSizingResponse sendFileToTomatoAPI(MultipartFile file){
         try {
            String filename = file.getOriginalFilename();
            boolean isXliff = filename != null && (filename.endsWith(".xliff") || filename.endsWith(".xlf"));
            String fileKey = isXliff ? "xliffFile" : "ditaFile";
            String endpoint = isXliff ? "/api/Sizing/sizing-from-xliff" : "/api/Sizing/sizing-from-dita";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(fileKey, new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    baseUrl + endpoint,
                    requestEntity,
                    String.class
            );

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            TomatoSizingResponse parsed = mapper.readValue(rawResponse.getBody(), TomatoSizingResponse.class);
            return parsed;
        } catch (Exception e) {
            System.err.println("Failed to send file to tomato API: " + e.getMessage());
            throw new RuntimeException("Failed to send file to tomato API", e);
        }
    }

    public TomatoSizingResponse sendFileToTomatoAPI(String filePath) {
        return sendFileToTomatoAPI(filePath, null, null);
    }

    public TomatoSizingResponse sendFileToTomatoAPI(String filePath, Map<MatchType, Long> netRatePercents) {
        return sendFileToTomatoAPI(filePath, netRatePercents, null);
    }

    public TomatoSizingResponse sendFileToTomatoAPI(String filePath, Map<MatchType, Long> netRatePercents, Long tmId) {
        Path path = Paths.get(uploadDir).resolve(filePath);
        String filename = path.getFileName().toString();

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file from path: " + filePath, e);
        }

        try {
            boolean isXliff = filename.endsWith(".xliff") || filename.endsWith(".xlf");
            String fileKey = isXliff ? "xliffFile" : "ditaFile";
            String endpoint = isXliff ? "/api/Sizing/sizing-from-xliff" : "/api/Sizing/sizing-from-dita";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(fileKey, new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });

            if (tmId != null) {
                body.add("tmId", String.valueOf(tmId));
            }

            if (netRatePercents != null) {
                String sizingRequestJson = String.format(
                        "{\"repetitionsPercent\":%d,\"percent101Percent\":%d,\"percent100Percent\":%d,"
                        + "\"percent95Percent\":%d,\"percent85Percent\":%d,\"percent75Percent\":%d,"
                        + "\"percent50Percent\":%d,\"percent0Percent\":%d}",
                        netRatePercents.getOrDefault(MatchType.REPETITIONS, 0L),
                        netRatePercents.getOrDefault(MatchType.PERCENT_101, 0L),
                        netRatePercents.getOrDefault(MatchType.PERCENT_100, 0L),
                        netRatePercents.getOrDefault(MatchType.PERCENT_95, 0L),
                        netRatePercents.getOrDefault(MatchType.PERCENT_85, 0L),
                        netRatePercents.getOrDefault(MatchType.PERCENT_75, 0L),
                        netRatePercents.getOrDefault(MatchType.PERCENT_50, 0L),
                        netRatePercents.getOrDefault(MatchType.PERCENT_0, 0L));
                body.add("sizingRequestJson", sizingRequestJson);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    baseUrl + endpoint,
                    requestEntity,
                    String.class
            );

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            TomatoSizingResponse parsed = mapper.readValue(rawResponse.getBody(), TomatoSizingResponse.class);
            return parsed;
        } catch (Exception e) {
            System.err.println("Failed to send file to tomato API: " + e.getMessage());
            throw new RuntimeException("Failed to send file to tomato API", e);
        }
    }
}
