package com.togettoyou.fabricrealty.springbootserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.togettoyou.fabricrealty.springbootserver.api.dto.BlockQueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.CreateRealEstateRequest;
import com.togettoyou.fabricrealty.springbootserver.api.dto.QueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.RealEstateDto;
import com.togettoyou.fabricrealty.springbootserver.block.storage.BlockStorage;
import com.togettoyou.fabricrealty.springbootserver.common.ApiException;
import com.togettoyou.fabricrealty.springbootserver.fabric.FabricClient;
import com.togettoyou.fabricrealty.springbootserver.fabric.FabricErrorTranslator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class RealtyAgencyService {
    private static final String REALTY_ORG = "org1";

    private final FabricClient fabricClient;
    private final ObjectMapper objectMapper;
    private final BlockStorage blockStorage;

    public RealtyAgencyService(FabricClient fabricClient, ObjectMapper objectMapper, BlockStorage blockStorage) {
        this.fabricClient = fabricClient;
        this.objectMapper = objectMapper;
        this.blockStorage = blockStorage;
    }

    public void createRealEstate(CreateRealEstateRequest request) {
        String id = request.id() == null ? "" : request.id();
        String address = request.address() == null ? "" : request.address();
        double area = request.area() == null ? 0.0 : request.area();
        String owner = request.owner() == null ? "" : request.owner();
        String now = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        try {
            fabricClient.submit(
                    REALTY_ORG,
                    "CreateRealEstate",
                    id,
                    address,
                    String.format(Locale.ROOT, "%f", area),
                    owner,
                    now
            );
        } catch (Exception e) {
            throw new ApiException(500, "创建房产信息失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public RealEstateDto queryRealEstate(String id) {
        try {
            byte[] result = fabricClient.evaluate(REALTY_ORG, "QueryRealEstate", id);
            return objectMapper.readValue(result, RealEstateDto.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, "查询房产信息失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public QueryResultDto<RealEstateDto> queryRealEstateList(int pageSize, String bookmark, String status) {
        try {
            byte[] result = fabricClient.evaluate(
                    REALTY_ORG,
                    "QueryRealEstateList",
                    String.valueOf(pageSize),
                    bookmark,
                    status
            );
            return objectMapper.readValue(result, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new ApiException(500, "查询房产列表失败：" + FabricErrorTranslator.toUserMessage(e));
        }
    }

    public BlockQueryResultDto queryBlockList(int pageSize, int pageNum) {
        try {
            return blockStorage.queryBlocks(REALTY_ORG, pageSize, pageNum);
        } catch (Exception e) {
            throw new ApiException(500, "查询区块列表失败：" + e.getMessage());
        }
    }
}
