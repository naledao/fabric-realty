package com.togettoyou.fabricrealty.springbootserver.block.model;

import java.time.OffsetDateTime;

public record BlockRecord(
        long blockNum,
        String blockHash,
        String dataHash,
        String prevHash,
        int txCount,
        OffsetDateTime saveTime
) {
}

