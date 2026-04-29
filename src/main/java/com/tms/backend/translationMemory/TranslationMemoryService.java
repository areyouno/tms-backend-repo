package com.tms.backend.translationMemory;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TranslationMemoryService {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${tomato.api.url}")
    private String tomatoBaseUrl;

    public TranslationMemoryService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public ResponseEntity<String> importTmx(Long id, MultipartFile file) throws IOException {
        String externalUrl = tomatoBaseUrl + "/api/TM/" + id + "/import-tmx";

        Resource fileResource = new InputStreamResource(file.getInputStream()) {
            @Override
            public String getFilename() { return file.getOriginalFilename(); }

            @Override
            public long contentLength() { return file.getSize(); }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofMinutes(10))
                .build();

        return restTemplate.postForEntity(
                externalUrl,
                new HttpEntity<>(body, headers),
                String.class
        );
    }
}
