package com.togettoyou.fabricrealty.springbootserver.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 区块分页查询结果 DTO (Data Transfer Object)
 * <p>
 * 这是一个标准的“分页响应包装器”。
 * 前端请求列表数据时，后端不仅返回数据本身，还要返回总条数、当前页码等信息，
 * 以便前端组件（如表格的分页条）能够正确渲染。
 */
public record BlockQueryResultDto(

        // 1. 核心数据负载
        // 这里的 List<BlockDataDto> 就是我们刚才定义的那个区块详情对象。
        // 它存放的是当前这一页（比如第1页）查出来的几条区块数据。
        List<BlockDataDto> blocks,

        // 2. 总记录数
        // 数据库中一共有多少个区块。
        // 前端用它来计算总页数：总页数 = total / pageSize (向上取整)。
        int total,

        // 3. 每页大小
        // @JsonProperty 将 Java 的驼峰命名 pageSize 映射为 JSON 的 snake_case "page_size"。
        // 表示每页显示多少条数据（例如 10条 或 20条）。
        @JsonProperty("page_size")
        int pageSize,

        // 4. 当前页码
        // 映射为 "page_num"。
        // 表示当前返回的是第几页的数据（例如第 1 页）。
        @JsonProperty("page_num")
        int pageNum,

        // 5. 是否有更多数据
        // 映射为 "has_more"。
        // 这是一个辅助字段，方便前端判断是否需要显示“加载更多”按钮或禁用“下一页”按钮。
        // 逻辑通常是：if (pageNum * pageSize < total) return true;
        @JsonProperty("has_more")
        boolean hasMore
) {
}