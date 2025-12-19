package com.togettoyou.fabricrealty.springbootserver.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * 区块数据传输对象 (DTO)
 * <p>
 * 1. 使用 Java 14+ 的 record 关键字定义，表示这是一个不可变的数据载体。
 * 2. 编译器会自动生成全参构造函数、Getter方法、equals()、hashCode() 和 toString()。
 * 3. 主要用于将区块链（Hyperledger Fabric）中的区块详情序列化为 JSON 返回给前端。
 */
public record BlockDataDto(

        /* * @JsonProperty 注解的作用：
         * 指定 JSON 字段名与 Java 变量名之间的映射关系。
         * * 场景：通常数据库或 JSON 习惯用下划线命名（snake_case），
         * 而 Java 习惯用驼峰命名（camelCase）。
         * 这里将 JSON 中的 "block_num" 自动映射到 Java 的 blockNum。
         */
        @JsonProperty("block_num")
        long blockNum,        // 区块高度 / 区块编号（第几个区块）

        @JsonProperty("block_hash")
        String blockHash,     // 当前区块的 Hash 值（区块的唯一标识）

        @JsonProperty("data_hash")
        String dataHash,      // 区块内数据的 Hash 值（确保数据未被篡改）

        @JsonProperty("prev_hash")
        String prevHash,      // 前一个区块的 Hash 值（这是区块链“链”起来的关键）

        @JsonProperty("tx_count")
        int txCount,          // 该区块内包含的交易(Transaction)数量

        @JsonProperty("save_time")
        OffsetDateTime saveTime // 区块落库或生成的时间
        // 使用 OffsetDateTime 是为了包含时区偏移量，比 Date 更精确
) {
    // record 的大括号内通常是空的，除非你需要自定义校验逻辑
}