package com.chris.gotravels.ticketservice.controller;

import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.frameworks.idempotent.annotation.Idempotent;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentSceneEnum;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentTypeEnum;
import com.chris.gotravels.frameworks.log.annotation.ILog;
import com.chris.gotravels.frameworks.web.Results;
import com.chris.gotravels.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.chris.gotravels.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.chris.gotravels.ticketservice.dto.req.RefundTicketReqDTO;
import com.chris.gotravels.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.chris.gotravels.ticketservice.dto.resp.RefundTicketRespDTO;
import com.chris.gotravels.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.chris.gotravels.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.chris.gotravels.ticketservice.remote.dto.PayInfoRespDTO;
import com.chris.gotravels.ticketservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车票控制层
 */
@RestController
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    /**
     * 根据条件查询车票
     */
    @GetMapping("/api/ticket-service/ticket/query")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(
                ticketService.pageListTicketQueryV1(requestParam)
        );
    }

    /**
     * 购买车票
     */
    @ILog
    @Idempotent(
            uniqueKeyPrefix = "index12306-ticket:lock_purchase-tickets:",
            key = "T(com.chris.gotravels.frameworks.base.ApplicationContextHolder).getBean('environment').getProperty('unique-name', '')"
                    + "+'_'+"
                    + "T(com.chris.gotravels.frameworks.biz.user.core.UserContext).getUsername()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI,
            type = IdempotentTypeEnum.SPEL
    )
    @PostMapping("/api/ticket-service/ticket/purchase")
    public Result<TicketPurchaseRespDTO> purchaseTickets(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(
                ticketService.purchaseTicketsV1(requestParam)
        );
    }

    /**
     * 购买车票v2
     */
    @ILog
    @Idempotent(
            uniqueKeyPrefix = "index12306-ticket:lock_purchase-tickets:",
            key = "T(com.chris.gotravels.frameworks.base.ApplicationContextHolder).getBean('environment').getProperty('unique-name', '')"
                    + "+'_'+"
                    + "T(com.chris.gotravels.frameworks.biz.user.core.UserContext).getUsername()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI,
            type = IdempotentTypeEnum.SPEL
    )
    @PostMapping("/api/ticket-service/ticket/purchase/v2")
    public Result<TicketPurchaseRespDTO> purchaseTicketsV2(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(
                ticketService.purchaseTicketsV2(requestParam)
        );
    }

    /**
     * 取消车票订单
     */
    @ILog
    @PostMapping("/api/ticket-service/ticket/cancel")
    public Result<Void> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        ticketService.cancelTicketOrder(requestParam);
        return Results.success();
    }

    /**
     * 支付单详情查询
     */
    @GetMapping("/api/ticket-service/ticket/pay/query")
    public Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(
                ticketService.getPayInfo(orderSn)
        );
    }

    /**
     * 公共退款接口
     */
    @PostMapping("/api/ticket-service/ticket/refund")
    public Result<RefundTicketRespDTO> commonTicketRefund(@RequestBody RefundTicketReqDTO requestParam) {
        return Results.success(
                ticketService.commonTicketRefund(requestParam)
        );
    }
}
