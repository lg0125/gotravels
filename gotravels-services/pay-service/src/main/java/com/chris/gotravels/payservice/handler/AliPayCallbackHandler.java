package com.chris.gotravels.payservice.handler;

import com.chris.gotravels.frameworks.pattern.strategy.AbstractExecuteStrategy;
import com.chris.gotravels.payservice.common.enums.PayChannelEnum;
import com.chris.gotravels.payservice.common.enums.TradeStatusEnum;
import com.chris.gotravels.payservice.dto.PayCallbackReqDTO;
import com.chris.gotravels.payservice.dto.base.AliPayCallbackRequest;
import com.chris.gotravels.payservice.dto.base.PayCallbackRequest;
import com.chris.gotravels.payservice.handler.base.AbstractPayCallbackHandler;

import com.chris.gotravels.payservice.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里支付回调组件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class AliPayCallbackHandler extends AbstractPayCallbackHandler implements AbstractExecuteStrategy<PayCallbackRequest, Void> {
    private final PayService payService;

    @Override
    public void callback(PayCallbackRequest payCallbackRequest) {
        AliPayCallbackRequest aliPayCallBackRequest = payCallbackRequest.getAliPayCallBackRequest();

        PayCallbackReqDTO payCallbackRequestParam = PayCallbackReqDTO.builder()
                .status(TradeStatusEnum.queryActualTradeStatusCode(aliPayCallBackRequest.getTradeStatus()))
                .payAmount(aliPayCallBackRequest.getBuyerPayAmount())
                .tradeNo(aliPayCallBackRequest.getTradeNo())
                .gmtPayment(aliPayCallBackRequest.getGmtPayment())
                .orderSn(aliPayCallBackRequest.getOrderRequestId())
                .build();

        payService.callbackPay(payCallbackRequestParam);
    }

    @Override
    public String mark() {
        return PayChannelEnum.ALI_PAY.name();
    }

    public void execute(PayCallbackRequest requestParam) {
        callback(requestParam);
    }
}
