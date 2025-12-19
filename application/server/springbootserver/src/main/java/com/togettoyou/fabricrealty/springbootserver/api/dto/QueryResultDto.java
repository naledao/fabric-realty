package com.togettoyou.fabricrealty.springbootserver.api.dto;

import java.util.List;

/**
 * 通用分页查询结果 DTO
 * <p>
 * 该 Record 用于封装带有分页信息的查询结果。
 * 采用了泛型 &lt;T&gt; 设计，因此既可以用于返回 "房产信息列表"，也可以用于返回 "交易记录列表"。
 * 特别适配了 Hyperledger Fabric (CouchDB) 的书签分页机制。
 * </p>
 *
 * @param <T>                 结果集中包含的数据类型 (例如: RealEstate, Transaction 等)
 * @param records             当前页的数据记录列表
 * @param recordsCount        本次查询相关的记录统计数 (通常指元数据中的记录数信息)
 * @param bookmark            分页书签 (CouchDB 的游标)。客户端请求下一页时需带上此字符串，为空表示已无更多数据。
 * @param fetchedRecordsCount 本次查询实际获取到的记录数量 (即 records.size())
 */
public record QueryResultDto<T>(
        // 结果数据集 (泛型列表)
        List<T> records,

        // 记录计数 (通常用于统计或验证)
        int recordsCount,

        // 分页书签/锚点 (关键字段：用于获取下一页数据)
        String bookmark,

        // 实际抓取的记录数 (应等于 records.size())
        int fetchedRecordsCount
) {
}