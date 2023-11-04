package com.chris.gotravels.ticketservice.service.impl;

import com.chris.gotravels.frameworks.base.ApplicationContextHolder;
import com.chris.gotravels.frameworks.biz.user.core.UserContext;
import com.chris.gotravels.frameworks.cache.DistributedCache;
import com.chris.gotravels.frameworks.cache.toolkit.CacheUtil;
import com.chris.gotravels.frameworks.common.toolkit.BeanUtil;
import com.chris.gotravels.frameworks.convention.exception.ServiceException;
import com.chris.gotravels.frameworks.convention.result.Result;
import com.chris.gotravels.frameworks.pattern.chain.AbstractChainContext;
import com.chris.gotravels.ticketservice.common.enums.*;
import com.chris.gotravels.ticketservice.dao.entity.*;
import com.chris.gotravels.ticketservice.dao.mapper.*;
import com.chris.gotravels.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.chris.gotravels.ticketservice.dto.domain.RouteDTO;
import com.chris.gotravels.ticketservice.dto.domain.SeatClassDTO;
import com.chris.gotravels.ticketservice.dto.domain.TicketListDTO;
import com.chris.gotravels.ticketservice.dto.req.*;
import com.chris.gotravels.ticketservice.dto.resp.RefundTicketRespDTO;
import com.chris.gotravels.ticketservice.dto.resp.TicketOrderDetailRespDTO;
import com.chris.gotravels.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.chris.gotravels.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.chris.gotravels.ticketservice.remote.PayRemoteService;
import com.chris.gotravels.ticketservice.remote.TicketOrderRemoteService;
import com.chris.gotravels.ticketservice.remote.dto.*;
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
import com.chris.gotravels.ticketservice.service.handler.dto.TrainPurchaseTicketRespDTO;
import com.chris.gotravels.ticketservice.service.handler.select.TrainSeatTypeSelector;
import com.chris.gotravels.ticketservice.service.handler.tokenbucket.TicketAvailabilityTokenBucket;
import com.chris.gotravels.ticketservice.toolkit.DateUtil;
import com.chris.gotravels.ticketservice.toolkit.TimeStringComparator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.chris.gotravels.ticketservice.common.constant.GotravelsConstant.ADVANCE_TICKET_DAY;
import static com.chris.gotravels.ticketservice.common.constant.RedisKeyConstant.*;
import static com.chris.gotravels.ticketservice.toolkit.DateUtil.convertDateToLocalTime;


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

    @Value("${ticket.availability.cache-update.type:}")
    private String ticketAvailabilityCacheUpdateType;

    @Value("${framework.cache.redis.prefix:}")
    private String cacheRedisPrefix;

    private final Cache<String, ReentrantLock> localLockMap = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    private List<String> buildDepartureStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getDeparture).distinct().collect(Collectors.toList());
    }

    private List<String> buildArrivalStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getArrival).distinct().collect(Collectors.toList());
    }

    private List<Integer> buildSeatClassList(List<TicketListDTO> seatResults) {
        Set<Integer> resultSeatClassList = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            for (SeatClassDTO item : each.getSeatClassList()) {
                resultSeatClassList.add(item.getType());
            }
        }
        return resultSeatClassList.stream().toList();
    }

    private List<Integer> buildTrainBrandList(List<TicketListDTO> seatResults) {
        Set<Integer> trainBrandSet = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            if (StrUtil.isNotBlank(each.getTrainBrand())) {
                trainBrandSet.addAll(StrUtil.split(each.getTrainBrand(), ",").stream().map(Integer::parseInt).toList());
            }
        }
        return trainBrandSet.stream().toList();
    }

    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV1(TicketPageQueryReqDTO requestParam) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

        // I 验证数据是否正确

        // 责任链模式 验证城市名称是否存在、不存在加载缓存以及出发日期不能小于当前日期等等
        // 通过责任链模式验证数据是否必填以及城市数据是否存在等执行逻辑
        // 通过责任链容器调用底层实现
        ticketPageQueryAbstractChainContext.handler(
                TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(),
                requestParam
        );

        // II 加载城市数据

        // 通过批量查询方式获取出发站点和到达站点对应的城市集合
        List<Object> stationDetails = stringRedisTemplate.opsForHash().multiGet(
                REGION_TRAIN_STATION_MAPPING,
                Lists.newArrayList(
                        requestParam.getFromStation(),
                        requestParam.getToStation()
                )
        );

        // 判空问题，同上验证查询数据
        long count = stationDetails.stream().filter(Objects::isNull).count();
        if (count > 0) {
            // 避免缓存击穿，通过分布式锁方式解决该问题
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION_MAPPING);

            lock.lock();
            try {
                // 双重判定锁，规避数据库无效请求
                stationDetails = stringRedisTemplate.opsForHash().multiGet(
                        REGION_TRAIN_STATION_MAPPING,
                        Lists.newArrayList(
                                requestParam.getFromStation(),
                                requestParam.getToStation()
                        )
                );

                count = stationDetails.stream().filter(Objects::isNull).count();
                if (count > 0) {
                    // 查询列车站点数据与城市信息
                    Map<String, String> regionTrainStationMap = new HashMap<>();

                    List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
                    stationDOList.forEach(
                            each -> regionTrainStationMap.put(
                                        each.getCode(),
                                        each.getRegionName()
                            )
                    );

                    // 通过 putAll 批量保存方式存入 Redis，避免多次 put 网络 IO 消耗
                    stringRedisTemplate.opsForHash().putAll(
                            REGION_TRAIN_STATION_MAPPING,
                            regionTrainStationMap
                    );

                    stationDetails = new ArrayList<>();
                    stationDetails.add(
                            regionTrainStationMap.get(requestParam.getFromStation())
                    );
                    stationDetails.add(
                            regionTrainStationMap.get(requestParam.getToStation())
                    );
                }
            } finally {
                lock.unlock();
            }
        }

        // III 查询列车站点信息

        List<TicketListDTO> seatResults = new ArrayList<>();

        // 构建查询 Redis Hash结构的Key，Key前缀 + 出发城市 + 到达城市
        String buildRegionTrainStationHashKey = String.format(
                REGION_TRAIN_STATION,
                stationDetails.get(0),
                stationDetails.get(1)
        );

        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
        // 如果为空，兜底查询数据库再放入缓存
        if (MapUtil.isEmpty(regionTrainStationAllMap)) {
            // 分布式锁
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION);

            lock.lock();
            try {
                regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
                // 双重判定锁DCL
                if (MapUtil.isEmpty(regionTrainStationAllMap)) {
                    // 加载数据库列车相关信息，并构建出一趟列车详细记录
                    LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getStartRegion, stationDetails.get(0))
                            .eq(TrainStationRelationDO::getEndRegion, stationDetails.get(1));

                    List<TrainStationRelationDO> trainStationRelationList = trainStationRelationMapper.selectList(queryWrapper);
                    for (var each : trainStationRelationList) {
                        TrainDO trainDO = distributedCache.safeGet(
                                TRAIN_INFO + each.getTrainId(),
                                TrainDO.class,
                                () -> trainMapper.selectById(each.getTrainId()),
                                ADVANCE_TICKET_DAY,
                                TimeUnit.DAYS
                        );

                        TicketListDTO result = new TicketListDTO();

                        result.setTrainId(String.valueOf(trainDO.getId()));
                        result.setTrainNumber(trainDO.getTrainNumber());
                        result.setDepartureTime(convertDateToLocalTime(each.getDepartureTime(), "HH:mm"));
                        result.setArrivalTime(convertDateToLocalTime(each.getArrivalTime(), "HH:mm"));
                        result.setDuration(DateUtil.calculateHourDifference(each.getDepartureTime(), each.getArrivalTime()));
                        result.setDeparture(each.getDeparture());
                        result.setArrival(each.getArrival());
                        result.setDepartureFlag(each.getDepartureFlag());
                        result.setArrivalFlag(each.getArrivalFlag());
                        result.setTrainType(trainDO.getTrainType());
                        result.setTrainBrand(trainDO.getTrainBrand());

                        if (StrUtil.isNotBlank(trainDO.getTrainTag()))
                            result.setTrainTags(
                                    StrUtil.split(trainDO.getTrainTag(), ",")
                            );

                        long betweenDay = cn.hutool.core.date.DateUtil.betweenDay(
                                each.getDepartureTime(),
                                each.getArrivalTime(),
                                false
                        );

                        result.setDaysArrived(
                                (int) betweenDay
                        );
                        result.setSaleStatus(
                                new Date().after(trainDO.getSaleTime())
                                        ? 0
                                        : 1
                        );
                        result.setSaleTime(
                                convertDateToLocalTime(
                                        trainDO.getSaleTime(),
                                        "MM-dd HH:mm"
                                )
                        );

                        seatResults.add(result);

                        regionTrainStationAllMap.put(
                                CacheUtil.buildKey(
                                        String.valueOf(each.getTrainId()),
                                        each.getDeparture(),
                                        each.getArrival()
                                ),
                                JSON.toJSONString(result)
                        );
                    }

                    // 全部加载完，批量保存到 Redis Hash 结构
                    stringRedisTemplate.opsForHash().putAll(
                            buildRegionTrainStationHashKey,
                            regionTrainStationAllMap
                    );
                }
            } finally {
                lock.unlock();
            }
        }

        // 查询列车基本信息之后，开始对列车按照出发时间进行排序
        seatResults = CollUtil.isEmpty(seatResults)
                ? regionTrainStationAllMap.values().stream()
                    .map(each -> JSON.parseObject(each.toString(), TicketListDTO.class))
                    .toList()
                : seatResults;

        seatResults = seatResults.stream()
                .sorted(new TimeStringComparator())
                .toList();

        // IV 查询列车余票信息

        // 列车基本信息已经全部填充完毕
        // 接下来,查询列车余票信息并填充到基本信息中
        // 列车余票数据是实时变更的，如果在存储到基本信息中，就没办法变更，所以单独存储
        for (TicketListDTO each : seatResults) {
            // 加载列车对应的座位价格数据
            // safeGet 就是安全获取缓存方案，底层加了分布式锁和双重判定锁
            // 如果查询 TRAIN_STATION_PRICE 数据为空，则加载数据库，并放入缓存
            String trainStationPriceStr = distributedCache.safeGet(
                    String.format(
                            TRAIN_STATION_PRICE,
                            each.getTrainId(),
                            each.getDeparture(),
                            each.getArrival()
                    ),

                    String.class,

                    () -> {
                        LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                                .eq(TrainStationPriceDO::getDeparture, each.getDeparture())
                                .eq(TrainStationPriceDO::getArrival, each.getArrival())
                                .eq(TrainStationPriceDO::getTrainId, each.getTrainId());

                        return JSON.toJSONString(
                                trainStationPriceMapper.selectList(trainStationPriceQueryWrapper)
                        );
                    },

                    ADVANCE_TICKET_DAY,

                    TimeUnit.DAYS
            );

            List<TrainStationPriceDO> trainStationPriceDOList = JSON.parseArray(
                    trainStationPriceStr,
                    TrainStationPriceDO.class
            );

            List<SeatClassDTO> seatClassList = new ArrayList<>();

            // 循环遍历座位价格数据，获取到座位对应的余票，并最终放入到列车基本信息中
            trainStationPriceDOList.forEach(item -> {
                String seatType = String.valueOf(item.getSeatType());

                String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());

                Object quantityObj = stringRedisTemplate.opsForHash().get(
                        TRAIN_STATION_REMAINING_TICKET + keySuffix,
                        seatType
                );

                int quantity = Optional.ofNullable(quantityObj)
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElseGet(() -> {
                            Map<String, String> seatMarginMap = seatMarginCacheLoader.load(
                                    String.valueOf(each.getTrainId()),
                                    seatType,
                                    item.getDeparture(),
                                    item.getArrival()
                            );

                            return Optional.ofNullable(
                                        seatMarginMap.get(String.valueOf(
                                                item.getSeatType()
                                        ))
                                    )
                                    .map(Integer::parseInt)
                                    .orElse(0);
                        });

                seatClassList.add(
                        new SeatClassDTO(
                                item.getSeatType(),
                                quantity,
                                new BigDecimal(item.getPrice()).divide(
                                        new BigDecimal("100"),
                                        1,
                                        RoundingMode.HALF_UP
                                ),
                                false
                        )
                );
            });
            each.setSeatClassList(seatClassList);
        }

        // V 构建列车返回数据

        // 通过构建者模式构建列车查询返回数据
        return TicketPageQueryRespDTO.builder()
                    .trainList(seatResults)
                    .departureStationList(buildDepartureStationList(seatResults))
                    .arrivalStationList(buildArrivalStationList(seatResults))
                    .trainBrandList(buildTrainBrandList(seatResults))
                    .seatClassTypeList(buildSeatClassList(seatResults))
                    .build();
    }

    // v2 版本更符合企业级高并发真实场景解决方案，完美解决了 v1 版本性能深渊问题
    // 通过 Jmeter 压测聚合报告得知，性能提升在 300% - 500%+
    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV2(TicketPageQueryReqDTO requestParam) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

        // I 验证数据是否正确

        // 责任链模式 验证城市名称是否存在、不存在加载缓存以及出发日期不能小于当前日期等等
        // 通过责任链模式验证数据是否必填以及城市数据是否存在等执行逻辑
        // 通过责任链容器调用底层实现
        ticketPageQueryAbstractChainContext.handler(
                TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(),
                requestParam
        );

        List<Object> stationDetails = stringRedisTemplate.opsForHash().multiGet(
                REGION_TRAIN_STATION_MAPPING,
                Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation())
        );

        String buildRegionTrainStationHashKey = String.format(
                REGION_TRAIN_STATION,
                stationDetails.get(0),
                stationDetails.get(1)
        );

        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);

        List<TicketListDTO> seatResults = regionTrainStationAllMap.values().stream()
                .map(each -> JSON.parseObject(each.toString(), TicketListDTO.class))
                .sorted(new TimeStringComparator())
                .toList();

        List<String> trainStationPriceKeys = seatResults.stream()
                .map(each -> String.format(
                        cacheRedisPrefix + TRAIN_STATION_PRICE,
                        each.getTrainId(),
                        each.getDeparture(),
                        each.getArrival()
                ))
                .toList();

        List<Object> trainStationPriceObjs = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {
            trainStationPriceKeys.forEach(
                    each -> connection.stringCommands().get(each.getBytes())
            );
            return null;
        });

        List<TrainStationPriceDO> trainStationPriceDOList = new ArrayList<>();

        List<String> trainStationRemainingKeyList = new ArrayList<>();

        for (var each : trainStationPriceObjs) {
            List<TrainStationPriceDO> trainStationPriceList = JSON.parseArray(each.toString(), TrainStationPriceDO.class);
            trainStationPriceDOList.addAll(trainStationPriceList);

            for (var item : trainStationPriceList) {
                String trainStationRemainingKey = cacheRedisPrefix + TRAIN_STATION_REMAINING_TICKET + StrUtil.join(
                        "_",
                        item.getTrainId(),
                        item.getDeparture(),
                        item.getArrival()
                );

                trainStationRemainingKeyList.add(trainStationRemainingKey);
            }
        }

        List<Object> TrainStationRemainingObjs = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {
            for (int i = 0; i < trainStationRemainingKeyList.size(); i++)
                connection.hashCommands().hGet(
                        trainStationRemainingKeyList.get(i).getBytes(),
                        trainStationPriceDOList.get(i).getSeatType().toString().getBytes()
                );
            return null;
        });

        for (var each : seatResults) {
            List<Integer> seatTypesByCode = VehicleTypeEnum.findSeatTypesByCode(each.getTrainType());

            List<Object> remainingTicket = new ArrayList<>(
                    TrainStationRemainingObjs.subList(
                            0,
                            seatTypesByCode.size()
                    )
            );

            List<TrainStationPriceDO> trainStationPriceDOSub = new ArrayList<>(
                    trainStationPriceDOList.subList(
                            0,
                            seatTypesByCode.size()
                    )
            );

            TrainStationRemainingObjs.subList(0, seatTypesByCode.size()).clear();
            trainStationPriceDOList.subList(0, seatTypesByCode.size()).clear();

            List<SeatClassDTO> seatClassList = new ArrayList<>();
            for (int i = 0; i < trainStationPriceDOSub.size(); i++) {
                TrainStationPriceDO trainStationPriceDO = trainStationPriceDOSub.get(i);

                SeatClassDTO seatClassDTO = SeatClassDTO.builder()
                        .type(trainStationPriceDO.getSeatType())
                        .quantity(
                                Integer.parseInt(
                                        remainingTicket.get(i).toString()
                                )
                        )
                        .price(
                                new BigDecimal(trainStationPriceDO.getPrice()).divide(
                                    new BigDecimal("100"),
                                    1,
                                    RoundingMode.HALF_UP
                                )
                        )
                        .candidate(false)
                        .build();

                seatClassList.add(seatClassDTO);
            }

            each.setSeatClassList(seatClassList);
        }
        return TicketPageQueryRespDTO.builder()
                    .trainList(seatResults)
                    .departureStationList(buildDepartureStationList(seatResults))
                    .arrivalStationList(buildArrivalStationList(seatResults))
                    .trainBrandList(buildTrainBrandList(seatResults))
                    .seatClassTypeList(buildSeatClassList(seatResults))
                    .build();
    }

    @Override
    public TicketPurchaseRespDTO purchaseTicketsV1(PurchaseTicketReqDTO requestParam) {
        // 验证提交参数
        // 用户在提交购票流程时，我们需要在座位分配以及创建订单前，判断用户提交的参数以及用户是否可以购买车票
        // 责任链模式，验证 1：参数必填 2：参数正确性 3：乘客是否已买当前车次等...
        purchaseTicketAbstractChainContext.handler(
                TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(),
                requestParam
        );

        String lockKey = environment.resolvePlaceholders(
                String.format(
                        LOCK_PURCHASE_TICKETS,
                        requestParam.getTrainId()
                )
        );

        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            return ticketService.executePurchaseTickets(requestParam);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TicketPurchaseRespDTO purchaseTicketsV2(PurchaseTicketReqDTO requestParam) {
        // 责任链模式，验证 1：参数必填 2：参数正确性 3：乘客是否已买当前车次等...
        purchaseTicketAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(), requestParam);

        boolean tokenResult = ticketAvailabilityTokenBucket.takeTokenFromBucket(requestParam);
        if (!tokenResult)
            throw new ServiceException("列车站点已无余票");

        // 存储本次请求需要获取的本地锁的集合
        List<ReentrantLock> localLockList = new ArrayList<>();

        // 存储本次请求需要获取的分布式锁的集合
        List<RLock> distributedLockList = new ArrayList<>();

        // 按照座位类型进行分组
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap =
                requestParam.getPassengers().stream().collect(
                        Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType)
                );

        seatTypeMap.forEach((searType, count) -> {
            // 构建锁 Key
            String lockKey = environment.resolvePlaceholders(
                    String.format(
                            LOCK_PURCHASE_TICKETS_V2,
                            requestParam.getTrainId(),
                            searType
                    )
            );

            ReentrantLock localLock = localLockMap.getIfPresent(lockKey);

            // 双重判定锁DCL(防止缓存击穿)
            if (localLock == null) {
                synchronized (TicketService.class) {
                    if ((localLock = localLockMap.getIfPresent(lockKey)) == null) {
                        localLock = new ReentrantLock(true);
                        localLockMap.put(lockKey, localLock);
                    }
                }
            }

            // 添加到本地锁集合
            localLockList.add(localLock);

            // 添加到分布式锁集合
            RLock distributedLock = redissonClient.getFairLock(lockKey);
            distributedLockList.add(distributedLock);
        });

        try {
            // 循环请求本地锁
            localLockList.forEach(ReentrantLock::lock);

            // 循环请求分布式锁
            distributedLockList.forEach(RLock::lock);

            return ticketService.executePurchaseTickets(requestParam);
        } finally {
            // 释放本地锁
            localLockList.forEach(localLock -> {
                try {
                    localLock.unlock();
                } catch (Throwable ignored) {
                }
            });

            // 释放分布式锁
            distributedLockList.forEach(distributedLock -> {
                try {
                    distributedLock.unlock();
                } catch (Throwable ignored) {
                }
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public TicketPurchaseRespDTO executePurchaseTickets(PurchaseTicketReqDTO requestParam) {
        List<TicketOrderDetailRespDTO> ticketOrderDetailResults = new ArrayList<>();

        String trainId = requestParam.getTrainId();

        // 节假日高并发购票Redis能扛得住么？
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + trainId,
                TrainDO.class,
                () -> trainMapper.selectById(trainId),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );

        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = trainSeatTypeSelector.select(
                trainDO.getTrainType(),
                requestParam
        );

        List<TicketDO> ticketDOList = trainPurchaseTicketResults.stream()
                .map(each -> TicketDO.builder()
                                .username(UserContext.getUsername())
                                .trainId(Long.parseLong(requestParam.getTrainId()))
                                .carriageNumber(each.getCarriageNumber())
                                .seatNumber(each.getSeatNumber())
                                .passengerId(each.getPassengerId())
                                .ticketStatus(TicketStatusEnum.UNPAID.getCode())
                                .build()
                )
                .toList();

        saveBatch(ticketDOList);

        Result<String> ticketOrderResult;
        try {

            List<TicketOrderItemCreateRemoteReqDTO> orderItemCreateRemoteReqDTOList = new ArrayList<>();

            trainPurchaseTicketResults.forEach(each -> {

                TicketOrderItemCreateRemoteReqDTO orderItemCreateRemoteReqDTO =
                        TicketOrderItemCreateRemoteReqDTO.builder()
                            .amount(each.getAmount())
                            .carriageNumber(each.getCarriageNumber())
                            .seatNumber(each.getSeatNumber())
                            .idCard(each.getIdCard())
                            .idType(each.getIdType())
                            .phone(each.getPhone())
                            .seatType(each.getSeatType())
                            .ticketType(each.getUserType())
                            .realName(each.getRealName())
                            .build();

                TicketOrderDetailRespDTO ticketOrderDetailRespDTO =
                        TicketOrderDetailRespDTO.builder()
                            .amount(each.getAmount())
                            .carriageNumber(each.getCarriageNumber())
                            .seatNumber(each.getSeatNumber())
                            .idCard(each.getIdCard())
                            .idType(each.getIdType())
                            .seatType(each.getSeatType())
                            .ticketType(each.getUserType())
                            .realName(each.getRealName())
                            .build();

                orderItemCreateRemoteReqDTOList.add(orderItemCreateRemoteReqDTO);

                ticketOrderDetailResults.add(ticketOrderDetailRespDTO);
            });

            LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, trainId)
                    .eq(TrainStationRelationDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationRelationDO::getArrival, requestParam.getArrival());

            TrainStationRelationDO trainStationRelationDO = trainStationRelationMapper.selectOne(queryWrapper);

            TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO =
                    TicketOrderCreateRemoteReqDTO.builder()
                        .departure(requestParam.getDeparture())
                        .arrival(requestParam.getArrival())
                        .orderTime(new Date())
                        .source(SourceEnum.INTERNET.getCode())
                        .trainNumber(trainDO.getTrainNumber())
                        .departureTime(trainStationRelationDO.getDepartureTime())
                        .arrivalTime(trainStationRelationDO.getArrivalTime())
                        .ridingDate(trainStationRelationDO.getDepartureTime())
                        .userId(UserContext.getUserId())
                        .username(UserContext.getUsername())
                        .trainId(Long.parseLong(requestParam.getTrainId()))
                        .ticketOrderItems(orderItemCreateRemoteReqDTOList)
                        .build();

            ticketOrderResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);

            if (!ticketOrderResult.isSuccess() || StrUtil.isBlank(ticketOrderResult.getData())) {
                log.error("订单服务调用失败，返回结果：{}", ticketOrderResult.getMessage());
                throw new ServiceException("订单服务调用失败");
            }
        } catch (Throwable ex) {
            log.error("远程调用订单服务创建错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
            throw ex;
        }

        return new TicketPurchaseRespDTO(
                ticketOrderResult.getData(),
                ticketOrderDetailResults
        );
    }

    @Override
    public PayInfoRespDTO getPayInfo(String orderSn) {
        return payRemoteService.getPayInfo(orderSn).getData();
    }

    @Override
    public void cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {
        Result<Void> cancelOrderResult = ticketOrderRemoteService.cancelTicketOrder(requestParam);

        if (cancelOrderResult.isSuccess() && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            Result<com.chris.gotravels.ticketservice.remote.dto.TicketOrderDetailRespDTO> ticketOrderDetailResult =
                    ticketOrderRemoteService.queryTicketOrderByOrderSn(requestParam.getOrderSn());

            com.chris.gotravels.ticketservice.remote.dto.TicketOrderDetailRespDTO ticketOrderDetail =
                    ticketOrderDetailResult.getData();

            String trainId = String.valueOf(ticketOrderDetail.getTrainId());
            String departure = ticketOrderDetail.getDeparture();
            String arrival = ticketOrderDetail.getArrival();

            List<TicketOrderPassengerDetailRespDTO> trainPurchaseTicketResults = ticketOrderDetail.getPassengerDetails();
            try {
                seatService.unlock(
                        trainId,
                        departure,
                        arrival,
                        BeanUtil.convert(
                                trainPurchaseTicketResults,
                                TrainPurchaseTicketRespDTO.class
                        )
                );
            } catch (Throwable ex) {
                log.error("[取消订单] 订单号：{} 回滚列车DB座位状态失败", requestParam.getOrderSn(), ex);
                throw ex;
            }

            ticketAvailabilityTokenBucket.rollbackInBucket(ticketOrderDetail);

            try {
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

                Map<Integer, List<TicketOrderPassengerDetailRespDTO>> seatTypeMap = trainPurchaseTicketResults.stream()
                        .collect(
                                Collectors.groupingBy(TicketOrderPassengerDetailRespDTO::getSeatType)
                        );

                List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);
                routeDTOList.forEach(each -> {
                    String keySuffix = StrUtil.join(
                            "_",
                            trainId,
                            each.getStartStation(),
                            each.getEndStation()
                    );

                    seatTypeMap.forEach(
                            (seatType, ticketOrderPassengerDetailRespDTOList) -> stringRedisTemplate.opsForHash().increment(
                                    TRAIN_STATION_REMAINING_TICKET + keySuffix,
                                    String.valueOf(seatType),
                                    ticketOrderPassengerDetailRespDTOList.size()
                            )
                    );
                });
            } catch (Throwable ex) {
                log.error("[取消关闭订单] 订单号：{} 回滚列车Cache余票失败", requestParam.getOrderSn(), ex);
                throw ex;
            }
        }
    }

    @Override
    public RefundTicketRespDTO commonTicketRefund(RefundTicketReqDTO requestParam) {
        // 责任链模式，验证 1：参数必填
        refundReqDTOAbstractChainContext.handler(
                TicketChainMarkEnum.TRAIN_REFUND_TICKET_FILTER.name(),
                requestParam
        );

        Result<com.chris.gotravels.ticketservice.remote.dto.TicketOrderDetailRespDTO> orderDetailRespDTOResult =
                ticketOrderRemoteService.queryTicketOrderByOrderSn(requestParam.getOrderSn());

        if (!orderDetailRespDTOResult.isSuccess() && Objects.isNull(orderDetailRespDTOResult.getData()))
            throw new ServiceException("车票订单不存在");

        com.chris.gotravels.ticketservice.remote.dto.TicketOrderDetailRespDTO ticketOrderDetailRespDTO =
                orderDetailRespDTOResult.getData();

        List<TicketOrderPassengerDetailRespDTO> passengerDetails = ticketOrderDetailRespDTO.getPassengerDetails();

        if (CollectionUtil.isEmpty(passengerDetails))
            throw new ServiceException("车票子订单不存在");

        RefundReqDTO refundReqDTO = new RefundReqDTO();

        if (RefundTypeEnum.PARTIAL_REFUND.getType().equals(requestParam.getType())) {
            TicketOrderItemQueryReqDTO ticketOrderItemQueryReqDTO = new TicketOrderItemQueryReqDTO();
            ticketOrderItemQueryReqDTO.setOrderSn(requestParam.getOrderSn());
            ticketOrderItemQueryReqDTO.setOrderItemRecordIds(requestParam.getSubOrderRecordIdReqList());

            Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById =
                    ticketOrderRemoteService.queryTicketItemOrderById(ticketOrderItemQueryReqDTO);

            List<TicketOrderPassengerDetailRespDTO> partialRefundPassengerDetails = passengerDetails.stream()
                    .filter(
                            item -> queryTicketItemOrderById.getData().contains(item)
                    )
                    .collect(Collectors.toList());

            refundReqDTO.setRefundTypeEnum(RefundTypeEnum.PARTIAL_REFUND);
            refundReqDTO.setRefundDetailReqDTOList(partialRefundPassengerDetails);
        } else if (RefundTypeEnum.FULL_REFUND.getType().equals(requestParam.getType())) {
            refundReqDTO.setRefundTypeEnum(RefundTypeEnum.FULL_REFUND);
            refundReqDTO.setRefundDetailReqDTOList(passengerDetails);
        }

        if (CollectionUtil.isNotEmpty(passengerDetails)) {
            Integer partialRefundAmount = passengerDetails.stream()
                    .mapToInt(TicketOrderPassengerDetailRespDTO::getAmount)
                    .sum();

            refundReqDTO.setRefundAmount(partialRefundAmount);
        }

        refundReqDTO.setOrderSn(requestParam.getOrderSn());

        Result<RefundRespDTO> refundRespDTOResult = payRemoteService.commonRefund(refundReqDTO);
        if (!refundRespDTOResult.isSuccess() && Objects.isNull(refundRespDTOResult.getData()))
            throw new ServiceException("车票订单退款失败");

        return null; // 暂时返回空实体
    }

    @Override
    public void run(String... args) {
        ticketService = ApplicationContextHolder.getBean(TicketService.class);
    }
}
