package com.togettoyou.fabricrealty.springbootserver.config;

public record OrganizationProperties(
        String mspId,
        String certPath,
        String keyPath,
        String tlsCertPath,
        String peerEndpoint,
        String gatewayPeer
) {
}

