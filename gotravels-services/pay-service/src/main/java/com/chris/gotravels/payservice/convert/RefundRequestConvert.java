package com.chris.gotravels.payservice.convert;

import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.payservice.common.enums.PayChannelEnum;
import com.chris.gotravels.payservice.dto.RefundCommand;
import com.chris.gotravels.payservice.dto.base.AliRefundRequest;
import com.chris.gotravels.payservice.dto.base.RefundRequest;

import java.util.Objects;

/**
 * 退款请求入参转换器
 */
public final class RefundRequestConvert {
    /**
     * {@link RefundCommand} to {@link RefundRequest}
     *
     * @param refundCommand 退款请求参数
     * @return {@link RefundRequest}
     */
    public static RefundRequest command2RefundRequest(RefundCommand refundCommand) {
        RefundRequest refundRequest = null;

        if (Objects.equals(
                refundCommand.getChannel(),
                PayChannelEnum.ALI_PAY.getCode()
        ))
            refundRequest = BeanUtil.convert(
                    refundCommand,
                    AliRefundRequest.class
            );

        return refundRequest;
    }
}
