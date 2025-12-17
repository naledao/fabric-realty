package com.togettoyou.fabricrealty.springbootserver.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record BlockDataDto(
        @JsonProperty("block_num") long blockNum,
        @JsonProperty("block_hash") String blockHash,
        @JsonProperty("data_hash") String dataHash,
        @JsonProperty("prev_hash") String prevHash,
        @JsonProperty("tx_count") int txCount,
        @JsonProperty("save_time") OffsetDateTime saveTime
) {
}

