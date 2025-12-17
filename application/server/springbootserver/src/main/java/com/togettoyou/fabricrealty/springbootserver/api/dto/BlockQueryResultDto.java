package com.togettoyou.fabricrealty.springbootserver.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BlockQueryResultDto(
        List<BlockDataDto> blocks,
        int total,
        @JsonProperty("page_size") int pageSize,
        @JsonProperty("page_num") int pageNum,
        @JsonProperty("has_more") boolean hasMore
) {
}

