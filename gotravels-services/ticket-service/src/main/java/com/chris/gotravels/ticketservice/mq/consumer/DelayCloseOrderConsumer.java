package com.chris.gotravels.ticketservice.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.ticketservice.common.constant.TicketRocketMQConstant;
import com.chris.gotravels.ticketservice.dto.domain.RouteDTO;
import com.chris.gotravels.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.chris.gotravels.ticketservice.mq.event.DelayCloseOrderEvent;
import com.chris.gotravels.ticketservice.remote.TicketOrderRemoteService;
import com.chris.gotravels.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.chris.gotravels.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.chris.gotravels.ticketservice.service.SeatService;
import com.chris.gotravels.ticketservice.service.TrainStationService;
import com.chris.gotravels.ticketservice.service.handler.dto.TrainPurchaseTicketRespDTO;
import com.chris.gotravels.ticketservice.service.handler.tokenbucket.TicketAvailabilityTokenBucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.chris.gotravels.ticketservice.mq.domain.MessageWrapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.chris.gotravels.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 延迟关闭订单消费者
 * <p>
 * 用户购买车票是先调用购票服务，然后购票服务调用订单服务创建订单。那么，此时已知购票服务依赖订单服务
 * <p>
 * 延时取消订单业务逻辑
 *      修改订单相关状态，比如将待支付变更为已取消
 *      解锁订单相关的座位状态，咱们下单时锁定了用户提交的座位状态为锁定状态，需要解锁
 *      回退订单中乘车人购买车座类型的缓存余票数量
 *      回退订单中乘车人购买车座类型的令牌限流数量
 * <p>
 * 延时关闭订单同时需要操作购票服务和订单服务，已知购票服务依赖订单服务，从微服务架构设计上，应尽量避免循环依赖问题
 * <p>
 * 将延时关闭订单的消费者放到购票服务(ticket-service)，再通过购票服务远程调用订单服务(order-service)修改订单状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.ORDER_DELAY_CLOSE_TOPIC_KEY,
        selectorExpression = TicketRocketMQConstant.ORDER_DELAY_CLOSE_TAG_KEY,
        consumerGroup = TicketRocketMQConstant.TICKET_DELAY_CLOSE_CG_KEY
)
public final class DelayCloseOrderConsumer
        implements RocketMQListener<MessageWrapper<DelayCloseOrderEvent>> {

    private final SeatService seatService;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;

    @Value("${ticket.availability.cache-update.type:}")
    private String ticketAvailabilityCacheUpdateType;

    @Override
    public void onMessage(MessageWrapper<DelayCloseOrderEvent> delayCloseOrderEventMessageWrapper) {
        log.info(
                "[延迟关闭订单] 开始消费：{}",
                JSON.toJSONString(delayCloseOrderEventMessageWrapper)
        );

        DelayCloseOrderEvent delayCloseOrderEvent = delayCloseOrderEventMessageWrapper.getMessage();

        String orderSn = delayCloseOrderEvent.getOrderSn();

        Result<Boolean> closedTickOrder;
        try {
            // 调用订单服务修改订单相关状态，比如将待支付变更为已取消
            closedTickOrder = ticketOrderRemoteService.closeTickOrder(new CancelTicketOrderReqDTO(orderSn));
        } catch (Throwable ex) {
            log.error("[延迟关闭订单] 订单号：{} 远程调用订单服务失败", orderSn, ex);
            throw ex;
        }

        // 订单取消成功没有报错并且返回标识也是成功
        if (closedTickOrder.isSuccess() && closedTickOrder.getData() && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {

            String trainId                                              = delayCloseOrderEvent.getTrainId();
            String departure                                            = delayCloseOrderEvent.getDeparture();
            String arrival                                              = delayCloseOrderEvent.getArrival();
            List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = delayCloseOrderEvent.getTrainPurchaseTicketResults();
            try {
                // 锁订单相关的座位状态，下单时设置了用户提交的座位状态为锁定状态，需要解锁
                seatService.unlock(
                        trainId,
                        departure,
                        arrival,
                        trainPurchaseTicketResults
                );
            } catch (Throwable ex) {
                log.error("[延迟关闭订单] 订单号：{} 回滚列车DB座位状态失败", orderSn, ex);
                throw ex;
            }

            try {
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

                Map<Integer, List<TrainPurchaseTicketRespDTO>> seatTypeMap = trainPurchaseTicketResults.stream()
                        .collect(Collectors.groupingBy(TrainPurchaseTicketRespDTO::getSeatType));

                List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);

                routeDTOList.forEach(each -> {
                    String keySuffix = StrUtil.join(
                            "_",
                            trainId,
                            each.getStartStation(),
                            each.getEndStation()
                    );

                    // 回退订单中乘车人购买车座类型的缓存余票数量
                    seatTypeMap.forEach(
                            (seatType, trainPurchaseTicketRespDTOList) -> stringRedisTemplate.opsForHash().increment(
                                        TRAIN_STATION_REMAINING_TICKET + keySuffix,
                                        String.valueOf(seatType),
                                        trainPurchaseTicketRespDTOList.size()
                            )
                    );
                });

                TicketOrderDetailRespDTO ticketOrderDetail = BeanUtil.convert(
                        delayCloseOrderEvent,
                        TicketOrderDetailRespDTO.class
                );

                ticketOrderDetail.setPassengerDetails(BeanUtil.convert(
                        delayCloseOrderEvent.getTrainPurchaseTicketResults(),
                        TicketOrderPassengerDetailRespDTO.class)
                );

                // 回退订单中乘车人购买车座类型的令牌限流数量
                ticketAvailabilityTokenBucket.rollbackInBucket(ticketOrderDetail);
            } catch (Throwable ex) {
                log.error("[延迟关闭订单] 订单号：{} 回滚列车Cache余票失败", orderSn, ex);
                throw ex;
            }
        }
    }
}
