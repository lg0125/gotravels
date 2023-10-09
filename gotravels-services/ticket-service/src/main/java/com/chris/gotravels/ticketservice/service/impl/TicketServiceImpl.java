package com.chris.gotravels.ticketservice.service.impl;

import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.frameworks.pattern.chain.AbstractChainContext;
import com.chris.gotravels.ticketservice.dao.entity.TicketDO;
import com.chris.gotravels.ticketservice.dao.mapper.*;
import com.chris.gotravels.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.chris.gotravels.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.chris.gotravels.ticketservice.dto.req.RefundTicketReqDTO;
import com.chris.gotravels.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.chris.gotravels.ticketservice.dto.resp.RefundTicketRespDTO;
import com.chris.gotravels.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.chris.gotravels.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.chris.gotravels.ticketservice.remote.PayRemoteService;
import com.chris.gotravels.ticketservice.remote.TicketOrderRemoteService;
import com.chris.gotravels.ticketservice.remote.dto.PayInfoRespDTO;
import com.chris.gotravels.ticketservice.service.SeatService;
import com.chris.gotravels.ticketservice.service.TicketService;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chris.gotravels.ticketservice.service.TrainStationService;
import com.chris.gotravels.ticketservice.service.cache.SeatMarginCacheLoader;
import com.chris.gotravels.ticketservice.service.handler.select.TrainSeatTypeSelector;
import com.chris.gotravels.ticketservice.service.handler.tokenbucket.TicketAvailabilityTokenBucket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;


/**
 * 车票接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, TicketDO> implements TicketService, CommandLineRunner {

    private final TrainMapper trainMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final DistributedCache distributedCache;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final PayRemoteService payRemoteService;
    private final StationMapper stationMapper;
    private final SeatService seatService;
    private final TrainStationService trainStationService;
    private final TrainSeatTypeSelector trainSeatTypeSelector;
    private final SeatMarginCacheLoader seatMarginCacheLoader;
    private final AbstractChainContext<TicketPageQueryReqDTO> ticketPageQueryAbstractChainContext;
    private final AbstractChainContext<PurchaseTicketReqDTO> purchaseTicketAbstractChainContext;
    private final AbstractChainContext<RefundTicketReqDTO> refundReqDTOAbstractChainContext;
    private final RedissonClient redissonClient;
    private final ConfigurableEnvironment environment;
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;
    private TicketService ticketService;

    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV1(TicketPageQueryReqDTO requestParam) {
        return null;
    }

    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV2(TicketPageQueryReqDTO requestParam) {
        return null;
    }

    @Override
    public TicketPurchaseRespDTO purchaseTicketsV1(PurchaseTicketReqDTO requestParam) {
        return null;
    }

    @Override
    public TicketPurchaseRespDTO purchaseTicketsV2(PurchaseTicketReqDTO requestParam) {
        return null;
    }

    @Override
    public TicketPurchaseRespDTO executePurchaseTickets(PurchaseTicketReqDTO requestParam) {
        return null;
    }

    @Override
    public PayInfoRespDTO getPayInfo(String orderSn) {
        return null;
    }

    @Override
    public void cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {

    }

    @Override
    public RefundTicketRespDTO commonTicketRefund(RefundTicketReqDTO requestParam) {
        return null;
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
