package com.chris.gotravels.orderservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chris.gotravels.orderservice.dao.entity.OrderItemDO;
import com.chris.gotravels.orderservice.dto.domain.OrderItemStatusReversalDTO;
import com.chris.gotravels.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;

import java.util.List;

/**
 * 订单明细接口层
 */
public interface OrderItemService extends IService<OrderItemDO> {
    /**
     * 子订单状态反转
     *
     * @param requestParam 请求参数
     */
    void orderItemStatusReversal(OrderItemStatusReversalDTO requestParam);

    /**
     * 根据子订单记录id查询车票子订单详情
     *
     * @param requestParam 请求参数
     */
    List<TicketOrderPassengerDetailRespDTO> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam);
}
