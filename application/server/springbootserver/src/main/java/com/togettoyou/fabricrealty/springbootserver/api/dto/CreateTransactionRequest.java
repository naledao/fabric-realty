package com.togettoyou.fabricrealty.springbootserver.api.dto;

public record CreateTransactionRequest(
        String txId,
        String realEstateId,
        String seller,
        String buyer,
        Double price
) {
}

