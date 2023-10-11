package com.chris.gotravels.orderservice.dao.entity;

import com.chris.gotravels.frameworks.database.base.BaseDO;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 乘车人订单关系实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_order_item_passenger")
public class OrderItemPassengerDO extends BaseDO {
    /**
     * id
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;
}
