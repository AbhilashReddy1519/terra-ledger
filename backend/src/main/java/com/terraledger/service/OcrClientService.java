package com.terraledger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OcrClientService {

    private static final Logger log = LoggerFactory.getLogger(OcrClientService.class);

    private final RestTemplate restTemplate;
    private final String ocrServiceUrl;
    private final ObjectMapper objectMapper;

    public OcrClientService(@Value("${app.ocr.service-url:http://localhost:8000}") String ocrServiceUrl,
                            ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.ocrServiceUrl = ocrServiceUrl;
        this.objectMapper = objectMapper;
    }

    public record OcrResult(String rawText, Double confidence, Map<String, String> fields) {}

    public OcrResult extract(byte[] fileBytes, String originalFilename) {
        try {
            String url = ocrServiceUrl + "/ocr/extract";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return originalFilename != null ? originalFilename : "document.png";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("OCR service returned non-2xx status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String rawText = root.path("raw_text").asText("");
            Double confidence = root.path("confidence").asDouble(0.85);

            Map<String, String> fields = new HashMap<>();
            JsonNode fieldsNode = root.path("fields");
            if (fieldsNode.isObject()) {
                fieldsNode.fields().forEachRemaining(entry -> {
                    fields.put(entry.getKey(), entry.getValue().asText(""));
                });
            }

            return new OcrResult(rawText, confidence, fields);
        } catch (Exception e) {
            log.error("Failed to perform OCR extraction via {}: {}", ocrServiceUrl, e.getMessage(), e);
            throw new RuntimeException("OCR extraction failed: " + e.getMessage(), e);
        }
    }
}
