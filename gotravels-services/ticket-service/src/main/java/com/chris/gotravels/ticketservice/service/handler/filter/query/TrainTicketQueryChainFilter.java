package com.chris.gotravels.ticketservice.service.handler.filter.query;

import com.chris.gotravels.frameworks.pattern.chain.AbstractChainHandler;
import com.chris.gotravels.ticketservice.common.enums.TicketChainMarkEnum;
import com.chris.gotravels.ticketservice.dto.req.TicketPageQueryReqDTO;

/**
 * 列车车票查询过滤器
 */
public interface TrainTicketQueryChainFilter <T extends TicketPageQueryReqDTO> extends AbstractChainHandler<TicketPageQueryReqDTO> {
    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_QUERY_FILTER.name();
    }
}
