package com.togettoyou.fabricrealty.springbootserver.api.dto;

/**
 * 创建房地产请求 DTO (Data Transfer Object)
 * <p>
 * 该 Record 用于封装客户端发起 "创建房产" 操作时传递的数据。
 * Java Record 是不可变的数据载体，自动包含构造函数、getter、equals、hashCode 和 toString 方法。
 * </p>
 *
 * @param id      房产唯一标识符 (通常对应链码中的 Key 或数据库的主键)
 * @param address 房产的详细物理地址
 * @param area    房产面积 (单位通常为平方米，使用 Double 以支持小数)
 * @param owner   房产所有人的标识 (如用户 ID 或姓名)
 */
public record CreateRealEstateRequest(
        // 房产唯一ID
        String id,

        // 房产地址
        String address,

        // 房产面积
        Double area,

        // 房产拥有者
        String owner
) {
}