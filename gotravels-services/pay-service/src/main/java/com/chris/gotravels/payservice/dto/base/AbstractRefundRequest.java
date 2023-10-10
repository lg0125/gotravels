package com.chris.gotravels.payservice.dto.base;

import lombok.Getter;
import lombok.Setter;

/**
 * 抽象退款入参实体
 */
@Getter
public abstract class AbstractRefundRequest implements RefundRequest{
    /**
     * 交易环境，H5、小程序、网站等
     */
    @Setter
    private Integer tradeType;

    /**
     * 订单号
     */
    @Setter
    private String orderSn;

    /**
     * 支付渠道
     */
    @Setter
    private Integer channel;

    @Override
    public AliRefundRequest getAliRefundRequest() {
        return null;
    }

    @Override
    public String buildMark() {
        return null;
    }
}
