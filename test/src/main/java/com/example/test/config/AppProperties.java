package com.example.test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置，对应 Express 侧 config/env.js
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Ai ai = new Ai();

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Ai getAi() {
        return ai;
    }

    public static class Jwt {
        private String secret = "development-secret-change-me-1234567890";
        private long expiresInSeconds = 7200;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiresInSeconds() {
            return expiresInSeconds;
        }

        public void setExpiresInSeconds(long expiresInSeconds) {
            this.expiresInSeconds = expiresInSeconds;
        }
    }

    public static class Cors {
        private String origins = "http://localhost:5173";

        public String getOrigins() {
            return origins;
        }

        public void setOrigins(String origins) {
            this.origins = origins;
        }
    }

    public static class Ai {
        private String gatewayBaseUrl = "https://aigateway.claudeoffice.com/v1";
        private String apiKey = "";
        private String model = "gpt-5.6-sol";

        public String getGatewayBaseUrl() {
            return gatewayBaseUrl;
        }

        public void setGatewayBaseUrl(String gatewayBaseUrl) {
            this.gatewayBaseUrl = gatewayBaseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
