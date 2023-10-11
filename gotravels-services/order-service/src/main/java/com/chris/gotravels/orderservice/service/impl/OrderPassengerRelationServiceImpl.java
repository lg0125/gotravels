package com.chris.gotravels.orderservice.service.impl;

import com.chris.gotravels.orderservice.dao.mapper.OrderItemPassengerMapper;
import com.chris.gotravels.orderservice.service.OrderPassengerRelationService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import com.chris.gotravels.orderservice.dao.entity.OrderItemPassengerDO;

/**
 * 乘车人订单关系接口层实现
 */
@Service
public class OrderPassengerRelationServiceImpl extends ServiceImpl<OrderItemPassengerMapper, OrderItemPassengerDO> implements OrderPassengerRelationService {
}
