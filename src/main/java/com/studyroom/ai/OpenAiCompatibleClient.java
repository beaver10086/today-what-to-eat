package com.studyroom.ai;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Optional OpenAI-compatible chat completion client; the app remains usable without credentials. */
@Component
public class OpenAiCompatibleClient {
    private final RestClient restClient = createClient();
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    private static RestClient createClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(20000);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    public OpenAiCompatibleClient(@Value("${app.ai.base-url:}") String baseUrl,
                                  @Value("${app.ai.api-key:}") String apiKey,
                                  @Value("${app.ai.model:}") String model) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
    }

    public boolean enabled() {
        return !baseUrl.isBlank() && !apiKey.isBlank() && !model.isBlank();
    }

    public String complete(String systemPrompt, String userPrompt, boolean jsonMode) {
        if (!enabled()) {
            return null;
        }
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0.2);
            body.put("messages", List.of(Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)));
            if (jsonMode) {
                body.put("response_format", Map.of("type", "json_object"));
            }
            com.fasterxml.jackson.databind.JsonNode response = restClient.post()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(com.fasterxml.jackson.databind.JsonNode.class);
            return response == null ? null : response.path("choices").path(0)
                    .path("message").path("content").asText(null);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
