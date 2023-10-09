package com.chris.gotravels.ticketservice.service.handler.filter.purchase;

import com.chris.gotravels.frameworks.pattern.chain.AbstractChainHandler;
import com.chris.gotravels.ticketservice.common.enums.TicketChainMarkEnum;
import com.chris.gotravels.ticketservice.dto.req.PurchaseTicketReqDTO;

/**
 * 列车购买车票过滤器
 */
public interface TrainPurchaseTicketChainFilter<T extends PurchaseTicketReqDTO> extends AbstractChainHandler<PurchaseTicketReqDTO> {
    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name();
    }
}
