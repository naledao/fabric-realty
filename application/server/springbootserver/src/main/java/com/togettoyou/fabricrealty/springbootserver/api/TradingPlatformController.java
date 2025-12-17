package com.togettoyou.fabricrealty.springbootserver.api;

import com.togettoyou.fabricrealty.springbootserver.api.dto.BlockQueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.CreateTransactionRequest;
import com.togettoyou.fabricrealty.springbootserver.api.dto.QueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.RealEstateDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.TransactionDto;
import com.togettoyou.fabricrealty.springbootserver.common.ApiResponse;
import com.togettoyou.fabricrealty.springbootserver.common.QueryParamUtils;
import com.togettoyou.fabricrealty.springbootserver.service.TradingPlatformService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trading-platform")
public class TradingPlatformController {
    private final TradingPlatformService service;

    public TradingPlatformController(TradingPlatformService service) {
        this.service = service;
    }

    @PostMapping("/transaction/create")
    public ApiResponse<Void> createTransaction(@RequestBody CreateTransactionRequest request) {
        service.createTransaction(request);
        return ApiResponse.successMessage("交易创建成功");
    }

    @GetMapping("/realty/{id}")
    public ApiResponse<RealEstateDto> queryRealEstate(@PathVariable String id) {
        return ApiResponse.success(service.queryRealEstate(id));
    }

    @GetMapping("/transaction/{txId}")
    public ApiResponse<TransactionDto> queryTransaction(@PathVariable String txId) {
        return ApiResponse.success(service.queryTransaction(txId));
    }

    @GetMapping("/transaction/list")
    public ApiResponse<QueryResultDto<TransactionDto>> queryTransactionList(
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "bookmark", required = false) String bookmark,
            @RequestParam(value = "status", required = false) String status
    ) {
        int pageSizeValue = QueryParamUtils.parseIntOrZero(pageSize == null ? "10" : pageSize);
        String bookmarkValue = bookmark == null ? "" : bookmark;
        String statusValue = status == null ? "" : status;
        return ApiResponse.success(service.queryTransactionList(pageSizeValue, bookmarkValue, statusValue));
    }

    @GetMapping("/block/list")
    public ApiResponse<BlockQueryResultDto> queryBlockList(
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "pageNum", required = false) String pageNum
    ) {
        int pageSizeValue = QueryParamUtils.parseIntOrZero(pageSize == null ? "10" : pageSize);
        int pageNumValue = QueryParamUtils.parseIntOrZero(pageNum == null ? "1" : pageNum);

        pageSizeValue = QueryParamUtils.normalizePositive(pageSizeValue, 10);
        pageNumValue = QueryParamUtils.normalizePositive(pageNumValue, 1);

        return ApiResponse.success(service.queryBlockList(pageSizeValue, pageNumValue));
    }
}

