package com.chris.gotravels.ticketservice.service.handler.filter.query;

import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.ticketservice.dto.req.TicketPageQueryReqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 查询列车车票流程过滤器之基础数据验证
 */
@Component
@RequiredArgsConstructor
public class TrainTicketQueryParamBaseVerifyChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {
    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        // 判断出发日期不能小于当前日期，毕竟不可能买上一天的车票，如果是的话，一定是异常请求
        if (requestParam.getDepartureDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now()))
            throw new ClientException("出发日期不能小于当前日期");

        if (Objects.equals(requestParam.getFromStation(), requestParam.getToStation()))
            throw new ClientException("出发地和目的地不能相同");
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
