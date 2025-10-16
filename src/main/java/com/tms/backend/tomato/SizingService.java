package com.tms.backend.tomato;

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

    public SizingService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public TomatoSizingResponse sendFileToTomatoAPI(MultipartFile file){
         try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("ditaFile", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<TomatoSizingResponse> response = restTemplate.postForEntity(
                    baseUrl + "/api/Sizing/sizing-from-dita",
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


}
