package com.togettoyou.fabricrealty.springbootserver.api.dto;

public record TransactionDto(
        String id,
        String realEstateId,
        String seller,
        String buyer,
        Double price,
        String status,
        String createTime,
        String updateTime
) {
}
