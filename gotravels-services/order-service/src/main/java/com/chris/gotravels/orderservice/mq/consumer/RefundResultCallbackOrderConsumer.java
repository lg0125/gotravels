package com.chris.gotravels.orderservice.mq.consumer;

import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.frameworks.idempotent.annotation.Idempotent;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentSceneEnum;
import com.chris.gotravels.frameworks.idempotent.enums.IdempotentTypeEnum;
import com.chris.gotravels.orderservice.common.constant.OrderRocketMQConstant;
import com.chris.gotravels.orderservice.common.enums.OrderItemStatusEnum;
import com.chris.gotravels.orderservice.common.enums.OrderStatusEnum;
import com.chris.gotravels.orderservice.dao.entity.OrderItemDO;
import com.chris.gotravels.orderservice.dto.domain.OrderItemStatusReversalDTO;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.chris.gotravels.orderservice.mq.domain.MessageWrapper;
import com.chris.gotravels.orderservice.mq.event.RefundResultCallbackOrderEvent;
import com.chris.gotravels.orderservice.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 退款结果回调订单消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic               = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression  = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_TAG_KEY,
        consumerGroup       = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_ORDER_CG_KEY
)
public class RefundResultCallbackOrderConsumer
        implements RocketMQListener<MessageWrapper<RefundResultCallbackOrderEvent>> {

    private final OrderItemService orderItemService;

    @Idempotent(
            uniqueKeyPrefix = "gotravels-order:refund_result_callback:",
            key = "#message.getKeys()+'_'+#message.hashCode()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.MQ,
            keyTimeout = 7200L
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<RefundResultCallbackOrderEvent> message) {

        RefundResultCallbackOrderEvent refundResultCallbackOrderEvent = message.getMessage();

        Integer status = refundResultCallbackOrderEvent.getRefundTypeEnum().getCode();
        String orderSn = refundResultCallbackOrderEvent.getOrderSn();

        List<OrderItemDO> orderItemDOList = new ArrayList<>();

        List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList =
                refundResultCallbackOrderEvent.getPartialRefundTicketDetailList();
        partialRefundTicketDetailList.forEach(partial -> {
            OrderItemDO orderItemDO = new OrderItemDO();
            BeanUtil.convert(partial, orderItemDO);
            orderItemDOList.add(orderItemDO);
        });

        if (status.equals(OrderStatusEnum.PARTIAL_REFUND.getStatus())) {
            OrderItemStatusReversalDTO partialRefundOrderItemStatusReversalDTO =
                    OrderItemStatusReversalDTO.builder()
                        .orderSn(orderSn)
                        .orderStatus(OrderStatusEnum.PARTIAL_REFUND.getStatus())
                        .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus())
                        .orderItemDOList(orderItemDOList)
                        .build();

            orderItemService.orderItemStatusReversal(partialRefundOrderItemStatusReversalDTO);
        } else if (status.equals(OrderStatusEnum.FULL_REFUND.getStatus())) {
            OrderItemStatusReversalDTO fullRefundOrderItemStatusReversalDTO =
                    OrderItemStatusReversalDTO.builder()
                        .orderSn(orderSn)
                        .orderStatus(OrderStatusEnum.FULL_REFUND.getStatus())
                        .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus())
                        .orderItemDOList(orderItemDOList)
                        .build();

            orderItemService.orderItemStatusReversal(fullRefundOrderItemStatusReversalDTO);
        }
    }
}
