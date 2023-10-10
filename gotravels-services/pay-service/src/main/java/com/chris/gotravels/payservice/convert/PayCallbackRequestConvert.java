package com.chris.gotravels.payservice.convert;

import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.payservice.common.enums.PayChannelEnum;
import com.chris.gotravels.payservice.dto.PayCallbackCommand;
import com.chris.gotravels.payservice.dto.base.AliPayCallbackRequest;
import com.chris.gotravels.payservice.dto.base.PayCallbackRequest;

import java.util.Objects;

/**
 * 支付回调请求入参转换器
 */
public final class PayCallbackRequestConvert {
    /**
     * {@link PayCallbackCommand} to {@link PayCallbackRequest}
     *
     * @param payCallbackCommand 支付回调请求参数
     * @return {@link PayCallbackRequest}
     */
    public static PayCallbackRequest command2PayCallbackRequest(PayCallbackCommand payCallbackCommand) {

        PayCallbackRequest payCallbackRequest = null;

        if (Objects.equals(payCallbackCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {

            payCallbackRequest = BeanUtil.convert(
                    payCallbackCommand,
                    AliPayCallbackRequest.class
            );

            ((AliPayCallbackRequest) payCallbackRequest).setOrderRequestId(
                    payCallbackCommand.getOrderRequestId()
            );
        }

        return payCallbackRequest;
    }
}
