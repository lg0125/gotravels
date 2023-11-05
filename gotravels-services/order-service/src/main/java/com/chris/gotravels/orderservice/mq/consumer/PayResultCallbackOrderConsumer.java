package com.chris.gotravels.orderservice.mq.consumer;

import com.chris.gotravels.frameworks.idempotent.annotation.Idempotent;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentSceneEnum;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentTypeEnum;
import com.chris.gotravels.orderservice.common.constant.OrderRocketMQConstant;
import com.chris.gotravels.orderservice.common.enums.OrderItemStatusEnum;
import com.chris.gotravels.orderservice.common.enums.OrderStatusEnum;
import com.chris.gotravels.orderservice.dto.domain.OrderStatusReversalDTO;
import com.chris.gotravels.orderservice.mq.domain.MessageWrapper;
import com.chris.gotravels.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.chris.gotravels.orderservice.service.OrderService;
import org.apache.rocketmq.spring.core.RocketMQListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付结果回调订单消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic               = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression  = OrderRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY,
        consumerGroup       = OrderRocketMQConstant.PAY_RESULT_CALLBACK_ORDER_CG_KEY
)
public class PayResultCallbackOrderConsumer
        implements RocketMQListener<MessageWrapper<PayResultCallbackOrderEvent>> {

    private final OrderService orderService;

    // 支付结果回调订单逻辑实现，通过 RocketMQMessageListener 监听并消费 RocketMQ 消息
    // 以实现支付结果回调订单为例，可以将通用组件引入到消息消费的逻辑中
    // 支持通过 SpEL 表达式来充当幂等去重表唯一键，通过一个简单的注解，完美解决消息队列重复消费问题
    @Idempotent(
            uniqueKeyPrefix = "index12306-order:pay_result_callback:",
            key = "#message.getKeys()+'_'+#message.hashCode()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.MQ,
            keyTimeout = 7200L
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<PayResultCallbackOrderEvent> message) {
        PayResultCallbackOrderEvent payResultCallbackOrderEvent = message.getMessage();

        OrderStatusReversalDTO orderStatusReversalDTO =
                OrderStatusReversalDTO.builder()
                    .orderSn(payResultCallbackOrderEvent.getOrderSn())
                    .orderStatus(OrderStatusEnum.ALREADY_PAID.getStatus())
                    .orderItemStatus(OrderItemStatusEnum.ALREADY_PAID.getStatus())
                    .build();

        orderService.statusReversal(orderStatusReversalDTO);

        orderService.payCallbackOrder(payResultCallbackOrderEvent);
    }
}
