package com.togettoyou.fabricrealty.springbootserver.api;

import com.togettoyou.fabricrealty.springbootserver.api.dto.BlockQueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.CreateRealEstateRequest;
import com.togettoyou.fabricrealty.springbootserver.api.dto.QueryResultDto;
import com.togettoyou.fabricrealty.springbootserver.api.dto.RealEstateDto;
import com.togettoyou.fabricrealty.springbootserver.common.ApiResponse;
import com.togettoyou.fabricrealty.springbootserver.common.QueryParamUtils;
import com.togettoyou.fabricrealty.springbootserver.service.RealtyAgencyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/realty-agency")
public class RealtyAgencyController {
    private final RealtyAgencyService service;

    public RealtyAgencyController(RealtyAgencyService service) {
        this.service = service;
    }

    @PostMapping("/realty/create")
    public ApiResponse<Void> createRealEstate(@RequestBody CreateRealEstateRequest request) {
        service.createRealEstate(request);
        return ApiResponse.successMessage("房产信息创建成功");
    }

    @GetMapping("/realty/{id}")
    public ApiResponse<RealEstateDto> queryRealEstate(@PathVariable String id) {
        return ApiResponse.success(service.queryRealEstate(id));
    }

    @GetMapping("/realty/list")
    public ApiResponse<QueryResultDto<RealEstateDto>> queryRealEstateList(
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "bookmark", required = false) String bookmark,
            @RequestParam(value = "status", required = false) String status
    ) {
        int pageSizeValue = QueryParamUtils.parseIntOrZero(pageSize == null ? "10" : pageSize);
        String bookmarkValue = bookmark == null ? "" : bookmark;
        String statusValue = status == null ? "" : status;
        return ApiResponse.success(service.queryRealEstateList(pageSizeValue, bookmarkValue, statusValue));
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

