package com.togettoyou.fabricrealty.springbootserver.api.dto;

/**
 * 创建交易请求 DTO
 * <p>
 * 该 Record 用于封装发起房地产交易（如买卖、转让）时所需的关键数据。
 * 在 Hyperledger Fabric 等区块链应用场景中，此对象通常对应链码中 invoke 交易所需的参数。
 * </p>
 *
 * @param txId          交易唯一标识符 (通常由前端生成的 UUID 或业务流水号，用于幂等性校验或追踪)
 * @param realEstateId  关联的房产唯一标识符 (指向被交易的资产)
 * @param seller        卖方标识 (当前房产所有者的用户 ID 或钱包地址)
 * @param buyer         买方标识 (接收房产的用户 ID 或钱包地址)
 * @param price         交易价格 (实际成交金额)
 */
public record CreateTransactionRequest(
        // 交易唯一ID (Transaction ID)
        String txId,

        // 房产ID (Real Estate ID)
        String realEstateId,

        // 卖方ID (Seller)
        String seller,

        // 买方ID (Buyer)
        String buyer,

        // 交易价格 (Transaction Price)
        Double price
) {
}