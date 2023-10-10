package com.chris.gotravels.payservice.convert;

import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.payservice.common.enums.PayChannelEnum;
import com.chris.gotravels.payservice.dto.PayCommand;
import com.chris.gotravels.payservice.dto.base.AliPayRequest;
import com.chris.gotravels.payservice.dto.base.PayRequest;

import java.util.Objects;

/**
 * 支付请求入参转换器
 */
public final class PayRequestConvert {
    /**
     * {@link PayCommand} to {@link PayRequest}
     *
     * @param payCommand 支付请求参数
     * @return {@link PayRequest}
     */
    public static PayRequest command2PayRequest(PayCommand payCommand) {
        PayRequest payRequest = null;

        if (Objects.equals(
                payCommand.getChannel(),
                PayChannelEnum.ALI_PAY.getCode()
        ))
            payRequest = BeanUtil.convert(
                    payCommand,
                    AliPayRequest.class
            );

        return payRequest;
    }
}
