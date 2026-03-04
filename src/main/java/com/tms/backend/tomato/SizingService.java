package com.tms.backend.tomato;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

            ResponseEntity<TomatoSizingResponse> response = restTemplate.postForEntity(
                    baseUrl + endpoint,
                    requestEntity,
                    TomatoSizingResponse.class
            );

            System.out.println("Upload response: " + response.getStatusCode() + " - " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Failed to send file to tomato API: " + e.getMessage());
            throw new RuntimeException("Failed to send file to tomato API", e);
        }
    }

    public TomatoSizingResponse sendFileToTomatoAPI(String filePath) {
        try {
            Path path = Paths.get(uploadDir).resolve(filePath);
            String filename = path.getFileName().toString();
            byte[] fileBytes = Files.readAllBytes(path);

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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<TomatoSizingResponse> response = restTemplate.postForEntity(
                    baseUrl + endpoint,
                    requestEntity,
                    TomatoSizingResponse.class
            );

            System.out.println("Upload response: " + response.getStatusCode() + " - " + response.getBody());
            return response.getBody();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file from path: " + filePath, e);
        } catch (Exception e) {
            System.err.println("Failed to send file to tomato API: " + e.getMessage());
            throw new RuntimeException("Failed to send file to tomato API", e);
        }
    }
}
