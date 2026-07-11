package com.legalai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Binds the `llm.*` properties from application.properties / env vars.
     * If llm.api-key is blank, AIAnalysisService automatically falls back
     * to the offline rule-based analyzer so the app still works with zero setup.
     */
    @Configuration
    @ConfigurationProperties(prefix = "llm")
    public static class LlmProperties {
        private String apiKey = "";
        private String apiUrl = "https://api.openai.com/v1/chat/completions";
        private String model = "gpt-4o-mini";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
}
