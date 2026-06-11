package com.tms.backend.termbase;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.backend.dto.TermbaseImportJobStatusDTO;
import com.tms.backend.dto.TermbaseImportStartResponseDTO;

@Service
public class TermbaseService {

    private static final Logger log = LoggerFactory.getLogger(TermbaseService.class);

    private final RestTemplate restTemplate;

    @Value("${tomato.api.url}")
    private String tomatoBaseUrl;

    public TermbaseService(RestTemplateBuilder restTemplateBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = restTemplateBuilder.requestFactory(() -> factory).build();
    }

    public String submitImport(Long termbaseId, MultipartFile file) throws IOException {
        String externalUrl = tomatoBaseUrl + "/api/Term/import";

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() { return file.getOriginalFilename(); }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("termbaseId", termbaseId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response = restTemplate.postForEntity(
                externalUrl,
                new HttpEntity<>(body, headers),
                String.class
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TermbaseImportStartResponseDTO dto = mapper.readValue(response.getBody(), TermbaseImportStartResponseDTO.class);
        log.info("Termbase import submitted for termbase {}, jobId: {}", termbaseId, dto.jobId());
        return dto.jobId();
    }

    public TermbaseImportJobStatusDTO fetchImportStatusOnce(String jobId) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    tomatoBaseUrl + "/api/Term/import/jobs/" + jobId,
                    String.class
            );
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(response.getBody(), TermbaseImportJobStatusDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Termbase import job not found: " + jobId, e);
        } catch (Exception e) {
            log.warn("Failed to fetch termbase import status for jobId {}: {}", jobId, e.getMessage());
            return null;
        }
    }
}
