package com.chris.gotravels.ticketservice.service.handler.filter.refund;

import com.chris.gotravels.frameworks.pattern.chain.AbstractChainHandler;
import com.chris.gotravels.ticketservice.common.enums.TicketChainMarkEnum;
import com.chris.gotravels.ticketservice.dto.req.RefundTicketReqDTO;

/**
 * 列车车票退款过滤器
 */
public interface TrainRefundTicketChainFilter<T extends RefundTicketReqDTO> extends AbstractChainHandler<RefundTicketReqDTO> {
    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_REFUND_TICKET_FILTER.name();
    }
}
