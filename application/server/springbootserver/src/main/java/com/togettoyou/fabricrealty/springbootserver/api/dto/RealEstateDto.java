package com.togettoyou.fabricrealty.springbootserver.api.dto;

/**
 * 房地产信息展示 DTO
 * <p>
 * 该 Record 用于向客户端（前端）返回完整的房产信息。
 * 包含了房产的基本属性、当前流转状态以及时间戳信息。
 * </p>
 *
 * @param id              房产唯一标识符 (Chaincode Key)
 * @param propertyAddress 房产物理地址
 * @param area            房产面积 (平方米)
 * @param currentOwner    当前拥有者 (User ID / MSP ID)
 * @param status          当前资产状态 (例如: "空闲/已领证", "交易中", "已售出")
 * @param createTime      创建时间 (格式化后的字符串，如 "2025-12-19 12:00:00")
 * @param updateTime      最后更新时间 (每次交易或状态变更时更新)
 */
public record RealEstateDto(
        // 房产唯一ID
        String id,

        // 房产地址 (注意字段名变成了 propertyAddress，与请求对象的 address 区分)
        String propertyAddress,

        // 房产面积
        Double area,

        // 当前拥有者ID
        String currentOwner,

        // 资产状态 (用于前端展示是否可购买、是否冻结等)
        String status,

        // 创建时间 (通常由后端将 LocalDateTime 或 Timestamp 格式化为 String 返回)
        String createTime,

        // 更新时间
        String updateTime
) {
}