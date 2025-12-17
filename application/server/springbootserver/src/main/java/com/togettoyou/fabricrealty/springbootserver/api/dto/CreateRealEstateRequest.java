package com.togettoyou.fabricrealty.springbootserver.api.dto;

public record CreateRealEstateRequest(
        String id,
        String address,
        Double area,
        String owner
) {
}

