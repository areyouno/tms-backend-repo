package com.tms.backend.tomato;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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

    public TomatoSizingResponse sendFilesToTomatoAPI(List<MultipartFile> files){
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

    public TomatoSizingResponse sendFilesToTomatoAPIByPath(List<String> filePaths) {
        return sendFilesToTomatoAPIByPath(filePaths, null, null);
    }

    public TomatoSizingResponse sendFilesToTomatoAPIByPath(List<String> filePaths, String sizingRequestJson, Long tmId) {
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
