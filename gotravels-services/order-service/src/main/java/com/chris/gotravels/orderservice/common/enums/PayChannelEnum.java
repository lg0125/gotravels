package com.chris.gotravels.orderservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * 支付渠道枚举
 */
@Getter
@RequiredArgsConstructor
public enum PayChannelEnum {
    /**
     * 支付宝
     */
    ALI_PAY(0, "ALI_PAY", "支付宝");

    private final Integer code;

    private final String name;

    private final String value;
}
