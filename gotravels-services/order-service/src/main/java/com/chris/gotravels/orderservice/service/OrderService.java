package com.chris.gotravels.orderservice.service;

import com.chris.gotravels.frameworks.convention.page.PageResponse;
import com.chris.gotravels.orderservice.dto.domain.OrderStatusReversalDTO;
import com.chris.gotravels.orderservice.dto.req.CancelTicketOrderReqDTO;
import com.chris.gotravels.orderservice.dto.req.TicketOrderCreateReqDTO;
import com.chris.gotravels.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import com.chris.gotravels.orderservice.dto.req.TicketOrderSelfPageQueryReqDTO;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.chris.gotravels.orderservice.mq.event.PayResultCallbackOrderEvent;


/**
 * 订单接口层
 */
public interface OrderService {
    /**
     * 跟据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return 订单详情
     */
    TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn);

    /**
     * 跟据用户名分页查询车票订单
     *
     * @param requestParam 跟据用户 ID 分页查询对象
     * @return 订单分页详情
     */
    PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam);

    /**
     * 创建火车票订单
     *
     * @param requestParam 商品订单入参
     * @return 订单号
     */
    String createTicketOrder(TicketOrderCreateReqDTO requestParam);

    /**
     * 关闭火车票订单
     *
     * @param requestParam 关闭火车票订单入参
     */
    boolean closeTickOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 取消火车票订单
     *
     * @param requestParam 取消火车票订单入参
     */
    boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 订单状态反转
     *
     * @param requestParam 请求参数
     */
    void statusReversal(OrderStatusReversalDTO requestParam);

    /**
     * 支付结果回调订单
     *
     * @param requestParam 请求参数
     */
    void payCallbackOrder(PayResultCallbackOrderEvent requestParam);

    /**
     * 查询本人车票订单
     *
     * @param requestParam 请求参数
     * @return 本人车票订单集合
     */
    PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam);
}
