package com.tms.backend.translationMemory;

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
import com.tms.backend.dto.ImportTmxRequestDTO;
import com.tms.backend.dto.TmxImportJobStatusDTO;
import com.tms.backend.dto.TmxImportStartResponseDTO;

@Service
public class TranslationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(TranslationMemoryService.class);

    private final RestTemplate restTemplate;

    @Value("${tomato.api.url}")
    private String tomatoBaseUrl;

    public TranslationMemoryService(RestTemplateBuilder restTemplateBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = restTemplateBuilder.requestFactory(() -> factory).build();
    }

    public String submitImportTmx(Long id, MultipartFile file, ImportTmxRequestDTO metadata) throws IOException {
        String externalUrl = tomatoBaseUrl + "/api/TM/" + id + "/import-tmx";

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() { return file.getOriginalFilename(); }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("tmxFile", fileResource);
        body.add("userName", metadata.userName());
        body.add("overwrite", metadata.overwrite());
        body.add("jobId", metadata.jobId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response = restTemplate.postForEntity(
                externalUrl,
                new HttpEntity<>(body, headers),
                String.class
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TmxImportStartResponseDTO dto = mapper.readValue(response.getBody(), TmxImportStartResponseDTO.class);
        log.info("TMX import submitted for TM {}, jobId: {}", id, dto.jobId());
        return dto.jobId();
    }

    public TmxImportJobStatusDTO fetchImportStatusOnce(String jobId) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    tomatoBaseUrl + "/api/TM/import-tmx/jobs/" + jobId,
                    String.class
            );
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(response.getBody(), TmxImportJobStatusDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Import job not found: " + jobId, e);
        } catch (Exception e) {
            log.warn("Failed to fetch import status for jobId {}: {}", jobId, e.getMessage());
            return null;
        }
    }
}
