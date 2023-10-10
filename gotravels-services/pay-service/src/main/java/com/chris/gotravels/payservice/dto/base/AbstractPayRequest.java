package com.chris.gotravels.payservice.dto.base;

import com.chris.gotravels.frameworks.id.toolkit.SnowflakeIdUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 抽象支付入参实体
 */
@Getter
public abstract class AbstractPayRequest implements PayRequest{
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

    /**
     * 商户订单号
     * 由商家自定义，64个字符以内，仅支持字母、数字、下划线且需保证在商户端不重复
     */
    @Setter
    private String orderRequestId = SnowflakeIdUtil.nextIdStr();

    @Override
    public AliPayRequest getAliPayRequest() {
        return null;
    }

    @Override
    public String getOrderRequestId() {
        return orderRequestId;
    }

    @Override
    public String buildMark() {
        return null;
    }
}
