package com.chris.gotravels.ticketservice.service.handler.filter.query;

import cn.hutool.core.util.StrUtil;
import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.springframework.stereotype.Component;

/**
 * 查询列车车票流程过滤器之验证数据是否为空或空的字符串
 * <p>
 * 检查相关数据是否为空或空的字符串，这个是最先被执行的
 * */
@Component
public class TrainTicketQueryParamNotNullChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO>{
    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {

        if (StrUtil.isBlank(requestParam.getFromStation()))
            throw new ClientException("出发地不能为空");

        if (StrUtil.isBlank(requestParam.getToStation()))
            throw new ClientException("目的地不能为空");

        if (requestParam.getDepartureDate() == null)
            throw new ClientException("出发日期不能为空");
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
