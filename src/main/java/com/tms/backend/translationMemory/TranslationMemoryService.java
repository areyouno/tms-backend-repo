package com.tms.backend.translationMemory;

import java.io.IOException;
import java.util.ArrayList;
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
import com.tms.backend.dto.TmAssignResponse;
import com.tms.backend.dto.TmCleanupResponseDTO;
import com.tms.backend.dto.TmListResponseDTO;
import com.tms.backend.dto.TmxImportJobStatusDTO;
import com.tms.backend.dto.TmxImportStartResponseDTO;
import com.tms.backend.dto.TranslationMemoryDTO;

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
        if (metadata.sourceLang() != null) {
            body.add("sourceLang", metadata.sourceLang());
        }
        if (metadata.targetLang() != null) {
            body.add("targetLang", metadata.targetLang());
        }

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

    public List<TranslationMemoryDTO> getFilteredTMs(String clientId, String sourceLang, String targetLang) throws IOException {
        return fetchAllTMs().stream()
                .filter(tm -> tm.file() != null)
                .filter(tm -> clientId == null || clientId.equalsIgnoreCase(tm.client()))
                .filter(tm -> sourceLang == null || sourceLang.equalsIgnoreCase(tm.sourceLanguage()))
                .filter(tm -> targetLang == null || targetLang.equalsIgnoreCase(tm.targetLanguage()))
                .toList();
    }

    private List<TranslationMemoryDTO> fetchAllTMs() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<TranslationMemoryDTO> allTms = new ArrayList<>();
        int page = 1;
        boolean hasNextPage;
        do {
            String url = tomatoBaseUrl + "/api/TM?page=" + page + "&pageSize=100";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            TmListResponseDTO listResponse = mapper.readValue(response.getBody(), TmListResponseDTO.class);
            if (listResponse.data() != null) {
                allTms.addAll(listResponse.data());
            }
            hasNextPage = listResponse.pagination() != null && Boolean.TRUE.equals(listResponse.pagination().hasNextPage());
            page++;
        } while (hasNextPage);

        return allTms;
    }

    public TmCleanupResponseDTO cleanupTm(Long tmId, MultipartFile xliffFile, String userName) throws IOException {
        String url = tomatoBaseUrl + "/api/TM/" + tmId + "/cleanup";

        ByteArrayResource fileResource = new ByteArrayResource(xliffFile.getBytes()) {
            @Override
            public String getFilename() { return xliffFile.getOriginalFilename(); }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("xliffFile", fileResource);
        body.add("userName", userName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), String.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TmCleanupResponseDTO dto = mapper.readValue(response.getBody(), TmCleanupResponseDTO.class);
        log.info("TM {} cleaned up: {} translation units merged by {}", tmId, dto.transUnitCount(), userName);
        return dto;
    }

    public byte[] exportTmx(Long tmId) {
        String url = tomatoBaseUrl + "/api/TM/" + tmId + "/export-tmx";
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        return response.getBody();
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

    /**
     * Calls Tomato's POST /api/TM/projects/{projectId}/assign to create/reuse a personal TM
     * for the given assignee and language pair, and assign it to a workflow step.
     */
    public TmAssignResponse assignPersonalTm(
            Long projectId, String sourceLanguage, String targetLanguage, String tmName,
            String assignedUserId, String workflowStage, String userName) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("assignedUserId", assignedUserId);
            body.add("workflowStage", workflowStage);
            body.add("tmName", tmName);
            body.add("sourceLanguage", sourceLanguage);
            body.add("targetLanguage", targetLanguage);
            body.add("userName", userName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    tomatoBaseUrl + "/api/TM/projects/" + projectId + "/assign", requestEntity, String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            TmAssignResponse result = mapper.readValue(rawResponse.getBody(), TmAssignResponse.class);
            log.info("Assigned personal TM for project {} ({} -> {}): tmId {} (wasExisting={})",
                    projectId, sourceLanguage, targetLanguage, result.tmId(), result.wasExisting());
            return result;
        } catch (Exception e) {
            log.error("Failed to assign personal TM for project {}: {}", projectId, e.getMessage());
            throw new RuntimeException("Failed to assign personal TM for project " + projectId, e);
        }
    }
}
