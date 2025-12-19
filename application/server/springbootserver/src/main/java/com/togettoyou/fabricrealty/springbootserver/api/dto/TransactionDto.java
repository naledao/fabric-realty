package com.togettoyou.fabricrealty.springbootserver.api.dto;

/**
 * 交易信息展示 DTO
 * <p>
 * 该 Record 用于向客户端（前端）展示单笔交易的详细信息。
 * 包含了买卖双方、价格以及交易当前的生命周期状态。
 * </p>
 *
 * @param id           交易唯一标识符 (Transaction ID，通常对应链上的 TxID)
 * @param realEstateId 关联的房产 ID (指向被交易的资产)
 * @param seller       卖方标识 (User ID / MSP ID)
 * @param buyer        买方标识 (User ID / MSP ID)
 * @param price        成交价格 (Double 类型，支持小数)
 * @param status       交易状态 (例如: "PENDING"-处理中, "CONFIRMED"-已确认, "INVALID"-无效)
 * @param createTime   交易创建时间 (字符串格式，如 "2025-12-19 14:30:00")
 * @param updateTime   交易最后更新时间
 */
public record TransactionDto(
        // 交易ID
        String id,

        // 关联的房产ID
        String realEstateId,

        // 卖方
        String seller,

        // 买方
        String buyer,

        // 交易价格
        Double price,

        // 交易状态 (关键字段：用于前端展示交易进度)
        String status,

        // 创建时间
        String createTime,

        // 更新时间
        String updateTime
) {
}