package com.togettoyou.fabricrealty.springbootserver.api.dto;

import java.util.List;

public record QueryResultDto<T>(
        List<T> records,
        int recordsCount,
        String bookmark,
        int fetchedRecordsCount
) {
}

