package com.chris.gotravels.orderservice.controller;

import cn.crane4j.annotation.AutoOperate;
import com.chris.gotravels.frameworks.convention.page.PageResponse;
import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.frameworks.web.Results;
import com.chris.gotravels.orderservice.dto.req.*;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.chris.gotravels.orderservice.service.OrderItemService;
import com.chris.gotravels.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 车票订单接口控制层
 */
@RestController
@RequiredArgsConstructor
public class TicketOrderController {

    private final OrderService orderService;

    private final OrderItemService orderItemService;

    /**
     * 根据订单号查询车票订单
     */
    @GetMapping("/api/order-service/order/ticket/query")
    public Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(
                orderService.queryTicketOrderByOrderSn(orderSn)
        );
    }

    /**
     * 根据子订单记录id查询车票子订单详情
     */
    @GetMapping("/api/order-service/order/item/ticket/query")
    public Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam) {
        return Results.success(
                orderItemService.queryTicketItemOrderById(requestParam)
        );
    }

    /**
     * 分页查询车票订单
     */
    @AutoOperate(type = TicketOrderDetailRespDTO.class, on = "data.records")
    @GetMapping("/api/order-service/order/ticket/page")
    public Result<PageResponse<TicketOrderDetailRespDTO>> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        return Results.success(
                orderService.pageTicketOrder(requestParam)
        );
    }

    /**
     * 分页查询本人车票订单
     */
    @GetMapping("/api/order-service/order/ticket/self/page")
    public Result<PageResponse<TicketOrderDetailSelfRespDTO>> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        return Results.success(
                orderService.pageSelfTicketOrder(requestParam)
        );
    }

    /**
     * 车票订单创建
     */
    @PostMapping("/api/order-service/order/ticket/create")
    public Result<String> createTicketOrder(@RequestBody TicketOrderCreateReqDTO requestParam) {
        return Results.success(
                orderService.createTicketOrder(requestParam)
        );
    }

    /**
     * 车票订单关闭
     */
    @PostMapping("/api/order-service/order/ticket/close")
    public Result<Boolean> closeTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(
                orderService.closeTickOrder(requestParam)
        );
    }

    /**
     * 车票订单取消
     */
    @PostMapping("/api/order-service/order/ticket/cancel")
    public Result<Boolean> cancelTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(
                orderService.cancelTickOrder(requestParam)
        );
    }
}
