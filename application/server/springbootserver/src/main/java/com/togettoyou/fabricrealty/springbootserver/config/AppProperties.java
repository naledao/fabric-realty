package com.togettoyou.fabricrealty.springbootserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String dataDir, int blockRetrySeconds) {
    public AppProperties {
        if (dataDir == null || dataDir.isBlank()) {
            dataDir = "./data";
        }
    }
}

