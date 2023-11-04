package com.chris.gotravels.orderservice.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chris.gotravels.frameworks.biz.user.core.UserContext;
import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.frameworks.convention.exception.ClientException;
import com.chris.gotravels.frameworks.convention.exception.ServiceException;
import com.chris.gotravels.frameworks.convention.page.PageResponse;
import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.frameworks.database.toolkit.PageUtil;
import com.chris.gotravels.orderservice.common.enums.OrderCanalErrorCodeEnum;
import com.chris.gotravels.orderservice.common.enums.OrderItemStatusEnum;
import com.chris.gotravels.orderservice.common.enums.OrderStatusEnum;
import com.chris.gotravels.orderservice.dao.entity.OrderDO;
import com.chris.gotravels.orderservice.dao.entity.OrderItemDO;
import com.chris.gotravels.orderservice.dao.entity.OrderItemPassengerDO;
import com.chris.gotravels.orderservice.dao.mapper.OrderItemMapper;
import com.chris.gotravels.orderservice.dto.domain.OrderStatusReversalDTO;
import com.chris.gotravels.orderservice.dto.req.*;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.chris.gotravels.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.chris.gotravels.orderservice.mq.event.DelayCloseOrderEvent;
import com.chris.gotravels.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.chris.gotravels.orderservice.mq.produce.DelayCloseOrderSendProduce;
import com.chris.gotravels.orderservice.remote.UserRemoteService;
import com.chris.gotravels.orderservice.remote.dto.UserQueryActualRespDTO;
import com.chris.gotravels.orderservice.service.OrderItemService;
import com.chris.gotravels.orderservice.service.OrderPassengerRelationService;
import com.chris.gotravels.orderservice.service.OrderService;
import com.chris.gotravels.orderservice.dao.mapper.OrderMapper;
import com.chris.gotravels.orderservice.service.orderid.OrderIdGeneratorManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 订单服务接口层实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final OrderItemService orderItemService;

    private final OrderPassengerRelationService orderPassengerRelationService;

    private final RedissonClient redissonClient;

    private final DelayCloseOrderSendProduce delayCloseOrderSendProduce;

    private final UserRemoteService userRemoteService;

    @Override
    public TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);

        OrderDO orderDO = orderMapper.selectOne(queryWrapper);

        TicketOrderDetailRespDTO result = BeanUtil.convert(orderDO, TicketOrderDetailRespDTO.class);

        LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, orderSn);

        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);

        result.setPassengerDetails(
                BeanUtil.convert(
                        orderItemDOList,
                        TicketOrderPassengerDetailRespDTO.class
                )
        );

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, requestParam.getUserId())
                .in(OrderDO::getStatus, buildOrderStatusList(requestParam))
                .orderByDesc(OrderDO::getOrderTime);

        IPage<OrderDO> orderPage = orderMapper.selectPage(
                PageUtil.convert(requestParam),
                queryWrapper
        );

        return PageUtil.convert(orderPage, each -> {
            TicketOrderDetailRespDTO result = BeanUtil.convert(
                    each,
                    TicketOrderDetailRespDTO.class
            );

            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn());

            List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);

            result.setPassengerDetails(
                    BeanUtil.convert(
                            orderItemDOList,
                            TicketOrderPassengerDetailRespDTO.class
                    )
            );

            return result;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String createTicketOrder(TicketOrderCreateReqDTO requestParam) {
        // 通过基因法将用户 ID 融入到订单号
        String orderSn = OrderIdGeneratorManager.generateId(requestParam.getUserId());

        OrderDO orderDO = OrderDO.builder().orderSn(orderSn)
                .orderTime(requestParam.getOrderTime())
                .departure(requestParam.getDeparture())
                .departureTime(requestParam.getDepartureTime())
                .ridingDate(requestParam.getRidingDate())
                .arrivalTime(requestParam.getArrivalTime())
                .trainNumber(requestParam.getTrainNumber())
                .arrival(requestParam.getArrival())
                .trainId(requestParam.getTrainId())
                .source(requestParam.getSource())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .username(requestParam.getUsername())
                .userId(String.valueOf(requestParam.getUserId()))
                .build();

        orderMapper.insert(orderDO);

        List<TicketOrderItemCreateReqDTO> ticketOrderItems = requestParam.getTicketOrderItems();

        List<OrderItemDO> orderItemDOList = new ArrayList<>();

        List<OrderItemPassengerDO> orderPassengerRelationDOList = new ArrayList<>();

        ticketOrderItems.forEach(each -> {
            OrderItemDO orderItemDO = OrderItemDO.builder()
                    .trainId(requestParam.getTrainId())
                    .seatNumber(each.getSeatNumber())
                    .carriageNumber(each.getCarriageNumber())
                    .realName(each.getRealName())
                    .orderSn(orderSn)
                    .phone(each.getPhone())
                    .seatType(each.getSeatType())
                    .username(requestParam.getUsername()).amount(each.getAmount()).carriageNumber(each.getCarriageNumber())
                    .idCard(each.getIdCard())
                    .ticketType(each.getTicketType())
                    .idType(each.getIdType())
                    .userId(String.valueOf(requestParam.getUserId()))
                    .status(0)
                    .build();

            orderItemDOList.add(orderItemDO);

            OrderItemPassengerDO orderPassengerRelationDO = OrderItemPassengerDO.builder()
                    .idType(each.getIdType())
                    .idCard(each.getIdCard())
                    .orderSn(orderSn)
                    .build();

            orderPassengerRelationDOList.add(orderPassengerRelationDO);
        });

        orderItemService.saveBatch(orderItemDOList);

        orderPassengerRelationService.saveBatch(orderPassengerRelationDOList);

        try {
            // 发送 RocketMQ 延时消息，指定时间后取消订单
            DelayCloseOrderEvent delayCloseOrderEvent = DelayCloseOrderEvent.builder()
                    .trainId(String.valueOf(requestParam.getTrainId()))
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderSn(orderSn)
                    .trainPurchaseTicketResults(requestParam.getTicketOrderItems())
                    .build();

            // 创建订单并支付后延时关闭订单消息怎么办？
            // 调用自定义延迟关闭订单消息生产者发送延迟关闭订单消息
            SendResult sendResult = delayCloseOrderSendProduce.sendMessage(delayCloseOrderEvent);
            if (!Objects.equals(sendResult.getSendStatus(), SendStatus.SEND_OK))
                throw new ServiceException("投递延迟关闭订单消息队列失败");
        } catch (Throwable ex) {
            log.error(
                    "延迟关闭订单消息队列发送错误，请求参数：{}",
                    JSON.toJSONString(requestParam),
                    ex
            );

            throw ex;
        }

        return orderSn;
    }

    // 已经支付的订单肯定是不能被延时取消的，又不能去删除 RocketMQ 的延时消息
    // 在业务上找寻一些突破口，比如：延时消息照常执行，执行前判断订单状态，如果是已支付，则直接返回成功不执行后续的取消逻辑
    @Override
    public boolean closeTickOrder(CancelTicketOrderReqDTO requestParam) {
        String orderSn = requestParam.getOrderSn();

        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn)
                .select(OrderDO::getStatus);

        // 根据订单号获取订单
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);

        // 订单如果等于空或者订单状态不等于待支付返回 false
        // 原来是订单取消前进行了状态判断，如果是非待支付的状态直接返回 false，就不会进行下面的流程
        // 订单取消接口返回 false 之后，购票服务(ticket-service)判断返回值为 false 就不进行接下来的座位解锁、余票更新等操作
        if (Objects.isNull(orderDO) || orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus())
            return false;

        // 原则上订单关闭和订单取消这两个方法可以复用，为了区分未来考虑到的场景，这里对方法进行拆分但复用逻辑
        // 订单状态等于待支付执行订单取消逻辑
        return cancelTickOrder(requestParam);
    }

    @Override
    public boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam) {
        String orderSn = requestParam.getOrderSn();

        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);

        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        if (orderDO == null)
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus())
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);

        RLock lock = redissonClient.getLock(
                StrBuilder.create("order:canal:order_sn_").append(orderSn).toString()
        );
        if (!lock.tryLock())
            throw new ClientException(OrderCanalErrorCodeEnum.ORDER_CANAL_REPETITION_ERROR);

        try {
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(OrderStatusEnum.CLOSED.getStatus());
            updateOrderDO.setOrderSn(orderSn);

            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, orderSn);

            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            if (updateResult <= 0)
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);

            OrderItemDO updateOrderItemDO = new OrderItemDO();
            updateOrderItemDO.setStatus(OrderItemStatusEnum.CLOSED.getStatus());
            updateOrderItemDO.setOrderSn(orderSn);

            LambdaUpdateWrapper<OrderItemDO> updateItemWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, orderSn);

            int updateItemResult = orderItemMapper.update(updateOrderItemDO, updateItemWrapper);
            if (updateItemResult <= 0)
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);

        } finally {
            lock.unlock();
        }

        return true;
    }

    @Override
    public void statusReversal(OrderStatusReversalDTO requestParam) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());

        OrderDO orderDO = orderMapper.selectOne(queryWrapper);

        if (orderDO == null)
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus())
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);

        RLock lock = redissonClient.getLock(
                StrBuilder.create("order:status-reversal:order_sn_")
                        .append(requestParam.getOrderSn())
                        .toString()
        );
        if (!lock.tryLock())
            log.warn("订单重复修改状态，状态反转请求参数：{}", JSON.toJSONString(requestParam));

        try {
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(requestParam.getOrderStatus());

            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, requestParam.getOrderSn());

            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            if (updateResult <= 0)
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);

            OrderItemDO orderItemDO = new OrderItemDO();
            orderItemDO.setStatus(requestParam.getOrderItemStatus());

            LambdaUpdateWrapper<OrderItemDO> orderItemUpdateWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn());

            int orderItemUpdateResult = orderItemMapper.update(
                    orderItemDO,
                    orderItemUpdateWrapper
            );
            if (orderItemUpdateResult <= 0)
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void payCallbackOrder(PayResultCallbackOrderEvent requestParam) {
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setPayTime(requestParam.getGmtPayment());
        updateOrderDO.setPayType(requestParam.getChannel());

        LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());

        int updateResult = orderMapper.update(
                updateOrderDO,
                updateWrapper
        );
        if (updateResult <= 0)
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
    }

    @SuppressWarnings("unchecked")
    @Override
    public PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        Result<UserQueryActualRespDTO> userActualResp = userRemoteService.queryActualUserByUsername(UserContext.getUsername());

        LambdaQueryWrapper<OrderItemPassengerDO> queryWrapper = Wrappers.lambdaQuery(OrderItemPassengerDO.class)
                .eq(OrderItemPassengerDO::getIdCard, userActualResp.getData().getIdCard())
                .orderByDesc(OrderItemPassengerDO::getCreateTime);

        IPage<OrderItemPassengerDO> orderItemPassengerPage = orderPassengerRelationService.page(
                PageUtil.convert(requestParam),
                queryWrapper
        );

        return PageUtil.convert(orderItemPassengerPage, each -> {

            LambdaQueryWrapper<OrderDO> orderQueryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                    .eq(OrderDO::getOrderSn, each.getOrderSn());

            OrderDO orderDO = orderMapper.selectOne(orderQueryWrapper);

            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn())
                    .eq(OrderItemDO::getIdCard, each.getIdCard());

            OrderItemDO orderItemDO = orderItemMapper.selectOne(orderItemQueryWrapper);

            TicketOrderDetailSelfRespDTO actualResult = BeanUtil.convert(
                    orderDO,
                    TicketOrderDetailSelfRespDTO.class
            );

            BeanUtil.convertIgnoreNullAndBlank(
                    orderItemDO,
                    actualResult
            );

            return actualResult;
        });
    }

    private List<Integer> buildOrderStatusList(TicketOrderPageQueryReqDTO requestParam) {
        List<Integer> result = new ArrayList<>();

        switch (requestParam.getStatusType()) {
            case 0 -> result = ListUtil.of(
                    OrderStatusEnum.PENDING_PAYMENT.getStatus()
            );
            case 1 -> result = ListUtil.of(
                    OrderStatusEnum.ALREADY_PAID.getStatus(),
                    OrderStatusEnum.PARTIAL_REFUND.getStatus(),
                    OrderStatusEnum.FULL_REFUND.getStatus()
            );
            case 2 -> result = ListUtil.of(
                    OrderStatusEnum.COMPLETED.getStatus()
            );
        }

        return result;
    }
}
