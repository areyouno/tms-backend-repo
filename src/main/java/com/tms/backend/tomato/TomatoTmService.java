package com.tms.backend.tomato;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.backend.dto.TmTemplateAssignResponse;

@Service
public class TomatoTmService {

    private static final Logger log = LoggerFactory.getLogger(TomatoTmService.class);

    private final RestTemplate restTemplate;

    @Value("${tomato.api.url}")
    private String baseUrl;

    public TomatoTmService(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = builder.requestFactory(() -> factory).build();
    }

    /**
     * Calls Tomato's POST /api/TM/templates/{templateTmId}/assign to materialize a standby TM
     * from the given template and assign it to a workflow step/user.
     */
    public TmTemplateAssignResponse assignTemplate(
            Long templateTmId, String assignedUserId, String workflowStage, String userName) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("assignedUserId", assignedUserId);
            body.add("workflowStage", workflowStage);
            body.add("userName", userName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    baseUrl + "/api/TM/templates/" + templateTmId + "/assign", requestEntity, String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            TmTemplateAssignResponse result = mapper.readValue(rawResponse.getBody(), TmTemplateAssignResponse.class);
            log.info("Assigned template TM {} -> tmId {} (wasExisting={})",
                    templateTmId, result.tmId(), result.wasExisting());
            return result;
        } catch (Exception e) {
            log.error("Failed to assign template TM {}: {}", templateTmId, e.getMessage());
            throw new RuntimeException("Failed to assign template TM " + templateTmId, e);
        }
    }
}
