package com.togettoyou.fabricrealty.springbootserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.togettoyou.fabricrealty.springbootserver.api.dto.BlockQueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.QueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.TransactionDto;
import com.togettoyou.fabricrealty.springbootserver.block.storage.BlockStorage;
import com.togettoyou.fabricrealty.springbootserver.common.ApiException;
import com.togettoyou.fabricrealty.springbootserver.fabric.FabricClient;
import com.togettoyou.fabricrealty.springbootserver.fabric.FabricErrorTranslator;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BankService {
    private static final String BANK_ORG = "org2";

    private final FabricClient fabricClient;
    private final ObjectMapper objectMapper;
    private final BlockStorage blockStorage;

    public BankService(FabricClient fabricClient, ObjectMapper objectMapper, BlockStorage blockStorage) {
        this.fabricClient = fabricClient;
        this.objectMapper = objectMapper;
        this.blockStorage = blockStorage;
    }

    public void completeTransaction(String txId) {
        String now = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        try {
            fabricClient.submit(BANK_ORG, "CompleteTransaction", txId, now);
        } catch (Exception e) {
            throw new ApiException(500, "完成交易失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public TransactionDto queryTransaction(String txId) {
        try {
            byte[] result = fabricClient.evaluate(BANK_ORG, "QueryTransaction", txId);
            return objectMapper.readValue(result, TransactionDto.class);
        } catch (Exception e) {
            throw new ApiException(500, "查询交易信息失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public QueryResultDto<TransactionDto> queryTransactionList(int pageSize, String bookmark, String status) {
        try {
            byte[] result = fabricClient.evaluate(
                    BANK_ORG,
                    "QueryTransactionList",
                    String.valueOf(pageSize),
                    bookmark,
                    status
            );
            return objectMapper.readValue(result, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new ApiException(500, "查询交易列表失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public BlockQueryResultDto queryBlockList(int pageSize, int pageNum) {
        try {
            return blockStorage.queryBlocks(BANK_ORG, pageSize, pageNum);
        } catch (Exception e) {
            throw new ApiException(500, "查询区块列表失败：" + e.getMessage());
        }
    }
}
