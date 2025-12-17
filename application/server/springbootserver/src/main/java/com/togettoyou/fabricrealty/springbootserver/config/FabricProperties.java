package com.togettoyou.fabricrealty.springbootserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "fabric")
public record FabricProperties(
        String channelName,
        String chaincodeName,
        Map<String, OrganizationProperties> organizations
) {
}

