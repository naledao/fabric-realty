package com.togettoyou.fabricrealty.springbootserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.togettoyou.fabricrealty.springbootserver.api.dto.BlockQueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.CreateTransactionRequest;
import com.togettoyou.fabricrealty.springbootserver.api.dto.QueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.RealEstateDto;
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
import java.util.Locale;

@Service
public class TradingPlatformService {
    private static final String TRADE_ORG = "org3";

    private final FabricClient fabricClient;
    private final ObjectMapper objectMapper;
    private final BlockStorage blockStorage;

    public TradingPlatformService(FabricClient fabricClient, ObjectMapper objectMapper, BlockStorage blockStorage) {
        this.fabricClient = fabricClient;
        this.objectMapper = objectMapper;
        this.blockStorage = blockStorage;
    }

    public void createTransaction(CreateTransactionRequest request) {
        String txId = request.txId() == null ? "" : request.txId();
        String realEstateId = request.realEstateId() == null ? "" : request.realEstateId();
        String seller = request.seller() == null ? "" : request.seller();
        String buyer = request.buyer() == null ? "" : request.buyer();
        double price = request.price() == null ? 0.0 : request.price();
        String now = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        try {
            fabricClient.submit(
                    TRADE_ORG,
                    "CreateTransaction",
                    txId,
                    realEstateId,
                    seller,
                    buyer,
                    String.format(Locale.ROOT, "%f", price),
                    now
            );
        } catch (Exception e) {
            throw new ApiException(500, "生成交易失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public RealEstateDto queryRealEstate(String id) {
        try {
            byte[] result = fabricClient.evaluate(TRADE_ORG, "QueryRealEstate", id);
            return objectMapper.readValue(result, RealEstateDto.class);
        } catch (Exception e) {
            throw new ApiException(500, "查询房产信息失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public TransactionDto queryTransaction(String txId) {
        try {
            byte[] result = fabricClient.evaluate(TRADE_ORG, "QueryTransaction", txId);
            return objectMapper.readValue(result, TransactionDto.class);
        } catch (Exception e) {
            throw new ApiException(500, "查询交易信息失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public QueryResultDto<TransactionDto> queryTransactionList(int pageSize, String bookmark, String status) {
        try {
            byte[] result = fabricClient.evaluate(
                    TRADE_ORG,
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
            return blockStorage.queryBlocks(TRADE_ORG, pageSize, pageNum);
        } catch (Exception e) {
            throw new ApiException(500, "查询区块列表失败：" + e.getMessage());
        }
    }
}
