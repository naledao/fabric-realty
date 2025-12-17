package com.togettoyou.fabricrealty.springbootserver.api.dto;

public record RealEstateDto(
        String id,
        String propertyAddress,
        Double area,
        String currentOwner,
        String status,
        String createTime,
        String updateTime
) {
}
